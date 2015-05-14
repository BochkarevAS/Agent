package ru.dvo.iacp.is.iacpaas.mas.agents;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ru.dvo.iacp.is.iacpaas.common.exceptions.PlatformException;
import ru.dvo.iacp.is.iacpaas.mas.IRunningAuthority;
import ru.dvo.iacp.is.iacpaas.mas.messages.TaskMessage;
import ru.dvo.iacp.is.iacpaas.storage.IConcept;
import ru.dvo.iacp.is.iacpaas.storage.IInforesource;
import ru.dvo.iacp.is.iacpaas.storage.IRelation;
import ru.dvo.iacp.is.iacpaas.storage.exceptions.StorageException;
import ru.dvo.iacp.is.iacpaas.storage.generator.IConceptGenerator;
import ru.dvo.iacp.is.iacpaas.storage.generator.IInforesourceGenerator;

public final class MyAgentImpl extends MyAgent {
	
	private static final String MAP_PERSON = "карта пациента";
	private static final String HISTORY = "ИБ";
	private static final String ENTRY_PROPERTY = "входной параметр";
	private static final String DIAGNOSIS = "диагноз";
	private static final String DN = "ДН";
	private static final String PREPARATION = "препарат";
	private static final String APPOINT = "назначение";
	private static final String PASSPORT = "паспортная часть";
	private static final String DATE = "дата рожд";
	private static final String COMPLICATIONS ="осложнения";
	private static final String ACCOMPANYING = "сопутствующий";
	private static final String CLINICAL = "клинический";
	private static final String PRELIMINARY = "предварительный";
	private static final String ROOT = "объяснениеЛечения";
	private static final String INFO_HISTORY = "инфо из ИБ";
	private static final String RECOMMENDED = "рекомендуемые";
	private static final String DEFLECTABLE = "отклоняемые";
	private static final String WARNING = "предупреждения";
	private static final String INCOMPATIBLE = "несовместимая пара";
	private static final String DRUG = "ЛС";
	private static final String FEATURE = "особенность";
	private static final String AGE = "возраст";
	private static final String DISEASE = "заболевание";
	private static final String FARM_GROUP = "фармГруппа";
	private static final String THERAPY = "основная терапия";
	private static final String DRUGS = "вариант ДВ";
	private static final String EFFECT = "эффект";
	
	private MyAgentImpl link = this;
	private String nameHistory;
	private String namePerson;
	
	public MyAgentImpl(IRunningAuthority runningAuthority, IInforesource agentInforesource) throws StorageException {
		super(runningAuthority, agentInforesource);
		
		IInforesource[] inputs = runningAuthority.getInputs();
		IInforesource infoArchive = inputs[2]; 
		
		if (infoArchive == null) {
			info("Inputs inforesource not found");
			return;
		}
		
		IConcept[] allConceptsArchive = infoArchive.getAllConcepts();
		IConcept children = allConceptsArchive[0].gotoByMeta(ENTRY_PROPERTY); //Архив ИБ
		
		this.namePerson = children.gotoByMeta(MAP_PERSON).getValue().toString();
		this.nameHistory = children.gotoByMeta(HISTORY).getValue().toString();
	}
	
	public class Archive {
		
		private IConcept[] allConceptsArchive;
		private IConcept[] children;
		private IConcept person;
		private IConcept history;
		private ArrayList<String> feature;
		
		private IConcept getPerson() throws StorageException {
			for (int i = 0; i < children.length; i++) {
				if (children[i].getName().equals(namePerson)) {
					return children[i];
				}
			}
			throw new IllegalArgumentException("Карта пациента не найдена...");
		}
		
		private IConcept getHistory() throws StorageException {
			IConcept[] arrayHistory = person.nextSetByMeta(HISTORY);
			for (int i = 0; i < arrayHistory.length; i++) {
				if (arrayHistory[i].getName().equals(nameHistory)) {
					return person.gotoByMeta(HISTORY);
				}
			}
			throw new IllegalArgumentException("История болезни не найдена...");
		}

		public double getAge() throws StorageException {
			Date currentDate = new Date();
			SimpleDateFormat dateFormat = new SimpleDateFormat("y");
			String currentYear = dateFormat.format(currentDate);

			if (history.hasRelation(AGE)) {
				IConcept ageHistory = history.gotoByMeta(AGE);
				return ageHistory.getValue();
			}
			
			if (person.hasRelation(PASSPORT)) {
				IConcept passport = person.gotoByMeta(PASSPORT);
				if (passport.gotoByMeta(DATE) != null) {
					String year = dateFormat.format(passport.gotoByMeta(DATE).getValue());
					int parseCurrentYear = Integer.parseInt(currentYear);
					int parseYear = Integer.parseInt(year);
					double age = parseCurrentYear - parseYear;
					return age;
				}
			}
			return 0;
		}
		
		public String getDiagnosis() throws StorageException {
			if (history.hasRelation(DIAGNOSIS)) {						
				IConcept diagnosis = history.gotoByMeta(DIAGNOSIS);
				feature = new ArrayList<String>();
				
				if (diagnosis.hasRelation(COMPLICATIONS)) {
					IConcept[] complications = diagnosis.gotoByMeta(COMPLICATIONS).getChildren();
					for (int i = 0; i < complications.length; i++) {
						feature.add(complications[i].getValue().toString());
					}
				}
				
				if (diagnosis.hasRelation(ACCOMPANYING)) {
					IConcept[] accompanying = diagnosis.gotoByMeta(ACCOMPANYING).getChildren();
					for (int i = 0; i < accompanying.length; i++) {
						feature.add(accompanying[i].getValue().toString());
					}
				}
				
				if (diagnosis.hasRelation(CLINICAL)) {
					IConcept clinical = diagnosis.gotoByMeta(CLINICAL).getChildren()[0];
					return clinical.getValue().toString();
				}
				
				if (diagnosis.hasRelation(PRELIMINARY)) {
					IConcept preliminary = diagnosis.gotoByMeta(PRELIMINARY).getChildren()[0];
					return preliminary.getValue().toString();
				}
			}
			throw new IllegalArgumentException("Проблема с диагнозом...");
		}
	
		public String[] getFeature() {
			if (!feature.isEmpty()) {
				String[] array = feature.toArray(new String[feature.size()]);
				return array;
			}
			return null;
		}

		public String[] getSimptom() throws StorageException {
			if (history.hasRelation(DN)) {
				IConcept simptom = history.gotoByMeta(DN).getChildren()[0];
				IConcept[] children = simptom.getChildren();
				ArrayList<String> list = new ArrayList<String>();

				for (int i = 0; i < children.length; i++) {
					String value = children[i].getValue();
					list.add(value);
				}
				return list.toArray(new String[list.size()]);
			}
			return null;
		}
		
		public String[] getPreparation() throws StorageException {
			if (history.hasRelation(APPOINT)) {
				IConcept[] preparation = history.gotoByMeta(APPOINT).nextSetByMeta(PREPARATION);
				ArrayList<String> list = new ArrayList<String>();
				
				for (int i = 0; i < preparation.length; i++) {
					String value = preparation[i].getName();
					list.add(value);
				}
				return list.toArray(new String[list.size()]);
			}
			return null;
		}
		
		public Archive() throws StorageException {
			IInforesource[] inputs = runningAuthority.getInputs();
			IInforesource infoArchive = inputs[2]; 
			
			if (infoArchive == null) {
				info("Inputs inforesource not found");
				return;
			}
			
			allConceptsArchive = infoArchive.getAllConcepts();
			children = allConceptsArchive[0].nextSetByMeta(MAP_PERSON); //Архив ИБ
			
			person = getPerson();
			history = getHistory();
			
		}
	}
	
	public class Treatment {
		
		private IConcept[] allConceptsArchive;
		
		private IConcept[] children;
		
		private IConcept getDiagnosis(String diagnosis) throws StorageException {
			for (int i = 0; i < children.length; i++) {			
				if (children[i].getName().equals(diagnosis)) {
					return children[i];
				}
			}
			return null;
		}
		
		public HashMap<String, String[]> setGroupDrugs(String diagnosis) throws StorageException {
			IConcept treatment = getDiagnosis(diagnosis);
			
			if (treatment != null) {	
				HashMap<String, String[]> hash = new HashMap<String, String[]>();
				IConcept[] farmGroup = treatment.gotoByMeta(THERAPY).nextSetByMeta(FARM_GROUP);
				
				for (int i = 0; i < farmGroup.length; i++) {
					ArrayList<String> list = new ArrayList<String>();
					String key = farmGroup[i].getName();
					IConcept[] nameDrug = farmGroup[i].nextSetByMeta(DRUGS);
					
					for (int j = 0; j < nameDrug.length; j++) {
						list.add(nameDrug[j].getName());
					}
					hash.put(key, list.toArray(new String[list.size()]));
				}
				return hash;
			}
			throw new IllegalArgumentException("Такого диагноза нет...");
		}
		
		public Treatment() throws StorageException {
			IInforesource[] inputs = runningAuthority.getInputs();
			IInforesource infoTreatment = inputs[1]; 
			
			if (infoTreatment == null) {
				info("Inputs inforesource not found");
				return;
			}
			
			allConceptsArchive = infoTreatment.getAllConcepts();
			children = allConceptsArchive[0].nextSetByMeta(DISEASE); //Лечение БЗ
		}
		
	}
	
	public class Solver {
		
		private Treatment treatment = new Treatment();
		private Archive archive = new Archive();
		
		private String diagnosis;
		private String[] feature;
		private double age;
		
		private IInforesourceGenerator[] outputs = runningAuthority.getOutputs();
		private IInforesourceGenerator outInforesource = outputs[0];
	
		private IConceptGenerator generateAxiom;
		private IConceptGenerator generate;
		private IConceptGenerator history;
		private IConceptGenerator recommended;
		private IConceptGenerator deflectable;
		private IConceptGenerator warning;
		
		private void addDrug() throws StorageException {
			HashMap<String, String[]> groupDrug = treatment.setGroupDrugs(diagnosis);
			
			for (Map.Entry<String, String[]> entry : groupDrug.entrySet()) {
				String[] value = entry.getValue();
				String key = entry.getKey();
				
				for (int i = 0; i < value.length; i++) {
					IConceptGenerator recommendedDrug = recommended.generateWithName(DRUG, value[i]);
					recommendedDrug.generateWithValue(EFFECT, key);
				}	
			}
		}

		public void drawInterface() throws StorageException {
			if (outInforesource.generateFromAxiom().hasRelation(ROOT)) {
				IConcept axiom = outInforesource.getAxiom();
				IRelation[] incomingRelations = axiom.gotoByMeta(ROOT).getIncomingRelations();
				incomingRelations[0].delete(link);
			}
			
			generateAxiom = outInforesource.generateFromAxiom();
			generate = generateAxiom.generateWithName(ROOT, new Date() + " " + namePerson + " " + nameHistory);
			history = generate.generateWithName(INFO_HISTORY, INFO_HISTORY);

			recommended = generate.generateCopy(RECOMMENDED);
			deflectable = generate.generateCopy(DEFLECTABLE);
			warning = generate.generateCopy(WARNING);

			diagnosis = archive.getDiagnosis();
			history.generateWithValue(DIAGNOSIS, diagnosis);
			
			age = archive.getAge();
			history.generateWithValue(AGE, age);
			
			feature = archive.getFeature();
			if (feature != null) {
				for (int i = 0; i < feature.length; i++) {
					history.generateWithValue(FEATURE, feature[i]);
				}
			}
		} 
		
		public IInforesourceGenerator getOutInforesource() {
			return outInforesource;
		}
		
		public Solver() throws StorageException {
			if (outInforesource == null) {
				info("Inputs inforesource not found");
				return;
			}
			
			drawInterface();
			addDrug();
		}
	}
	
	public void runProduction(ru.dvo.iacp.is.iacpaas.mas.messages.TaskMessage msg, TaskMessageResultCreator rc) throws PlatformException {
		
		try {
			Solver solver = new Solver();
			IInforesourceGenerator outInforesource = solver.getOutInforesource();
			
			IConcept axiom = outInforesource.getAxiom();
			TaskMessage create = rc.taskMessage.create("Онтологии и свойства / область медицины / AgentFilter");
			create.setAgentTestReportInforesource(axiom);
			
		} catch (Exception e) {
			info(e.getMessage());
		}
	}
	
}
