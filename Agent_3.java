package ru.dvo.iacp.is.iacpaas.mas.agents;

import java.util.ArrayList;
import java.util.Date;

import ru.dvo.iacp.is.iacpaas.common.exceptions.PlatformException;
import ru.dvo.iacp.is.iacpaas.mas.IRunningAuthority;
import ru.dvo.iacp.is.iacpaas.storage.IConcept;
import ru.dvo.iacp.is.iacpaas.storage.IInforesource;
import ru.dvo.iacp.is.iacpaas.storage.IRelation;
import ru.dvo.iacp.is.iacpaas.storage.exceptions.StorageException;
import ru.dvo.iacp.is.iacpaas.storage.generator.IConceptGenerator;
import ru.dvo.iacp.is.iacpaas.storage.generator.IInforesourceGenerator;

public final class AgentTesterImpl extends AgentTester {

	private static final String MAP_PERSON = "карта пациента";
	private static final String HISTORY = "ИБ";
	private static final String ENTRY_PROPERTY = "входной параметр";
	private static final String APPOINT = "назначение";
	private static final String PREPARATION = "препарат";
	private static final String FEATURE_PERSON = "особенности пациента";
	private static final String PRECEDENT = "прецедент";
	private static final String APPOINTED_TREATMENT = "назначенное лечение";
	private static final String DRUG = "ЛС";
	private static final String CIPHER_HISTORY = "шифр ИБ";
	private static final String ROOT = "объяснениеЛечения";
	private static final String INFO_HISTORY = "инфо из ИБ";
	private static final String RECOMMENDED = "рекомендуемые";
	private static final String DEFLECTABLE = "отклоняемые";
	private static final String FEATURE = "особенность";
	private static final String FONE_TREATMENT = "фоновое заболевание";
	private static final String ECUELSE = "сравнение с объяснением";
	private static final String NO = "Нет среди рекомендуемых";
	private static final String YES = "Есть среди отвергнутых";
	
	private AgentTesterImpl link = this;
	private IConcept[] children;
	private IConcept root;
	private String namePerson;
	private String nameHistory;

	public AgentTesterImpl(IRunningAuthority runningAuthority, IInforesource agentInforesource) throws StorageException {
		super(runningAuthority, agentInforesource);

		IInforesource[] inputsList = runningAuthority.getInputs();
		IInforesource inputsArchive = inputsList[0];
		IInforesource inputsSolverTreatment = inputsList[1];

		if ((inputsArchive == null) || (inputsSolverTreatment == null)) {
			info("Inputs inforesource not found");
			return;
		}

		IConcept[] allArchive = inputsArchive.getAllConcepts();
		IConcept[] allSolverTreatment = inputsSolverTreatment.getAllConcepts();
		
		root = allSolverTreatment[0].gotoByMeta(ROOT); // Решатель объяснения
		children = allArchive[0].nextSetByMeta(MAP_PERSON); // Архив ИБ
		IConcept inputsArg = allArchive[0].gotoByMeta(ENTRY_PROPERTY); // Входной параметр
		
		this.namePerson = inputsArg.gotoByMeta(MAP_PERSON).getValue().toString();
		this.nameHistory = inputsArg.gotoByMeta(HISTORY).getValue().toString();
	}

	public class MainTest {

		private IInforesourceGenerator outInforesource;
		private IInforesourceGenerator[] outputs;
		private IConcept history;
		private IConcept person;
		private IConcept[] listFeature;
		private IConcept[] listRecommended;
		private IConcept[] listDeflectable;
		private String[] preparation;
		private IConceptGenerator generateAxiom;
		private IConceptGenerator generate;
		private IConceptGenerator appointed;
		private IConceptGenerator cipher;
		private IConceptGenerator feature;
		private IConceptGenerator ecuelse;
		private IConceptGenerator yes;
		private IConceptGenerator no;

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
		
		public void infoTreatment() throws StorageException {
			IConcept infoHistory = root.gotoByMeta(INFO_HISTORY);
			listFeature = infoHistory.nextSetByMeta(FEATURE);
			
			IConcept infoRecommended = root.gotoByMeta(RECOMMENDED);
			listRecommended = infoRecommended.getChildren(); 
			
			IConcept infoDeflectable = root.gotoByMeta(DEFLECTABLE);
			listDeflectable = infoDeflectable.getChildren(); 
		}
		
		private void addInteraction() throws StorageException {
			boolean flage = true;
			 
			if ((preparation != null) && (listRecommended != null)) {
				for (int i = 0; i < preparation.length; i++) {
					for (int j = 0; j < listRecommended.length; j++) {
						if (preparation[i].toLowerCase().equals(listRecommended[j].getName().toLowerCase())) {
							flage = false;
							break;
						} 
					}
					if (flage) no.generateWithValue(DRUG, preparation[i]);
					flage = true;
					 
					if (listDeflectable == null) continue;
					for (int j = 0; j < listDeflectable.length; j++) {
						if (preparation[i].toLowerCase().equals(listDeflectable[j].getName().toLowerCase())) {
							yes.generateWithValue(DRUG, preparation[i]);
						}
					}
				 }
			}
		}

		public void drawTestIntarface() throws StorageException {
			if (outInforesource.generateFromAxiom().hasRelation(PRECEDENT)) {
				IConcept axiom = outInforesource.getAxiom();
				IRelation[] incomingRelations = axiom.gotoByMeta(PRECEDENT).getIncomingRelations();
				incomingRelations[0].delete(link);
			}

			generateAxiom = outInforesource.generateFromAxiom();
			generate = generateAxiom.generateWithName(PRECEDENT, new Date() + " ");
			cipher = generate.generateWithValue(CIPHER_HISTORY, nameHistory);
			feature = generate.generateCopy(FEATURE_PERSON);
			appointed = generate.generateCopy(APPOINTED_TREATMENT);
			ecuelse = generate.generateCopy(ECUELSE);
			yes = ecuelse.generateCopy(YES);
			no = ecuelse.generateCopy(NO);

			if (preparation != null) {
				for (int i = 0; i < preparation.length; i++) {
					appointed.generateWithValue(DRUG, preparation[i]);
				}
			}
			
			if (listFeature != null) {
				for (int i = 0; i < listFeature.length; i++) {
					feature.generateWithValue(FONE_TREATMENT, listFeature[i].getValue().toString());
				}
			}

		}

		public MainTest() throws StorageException {
			outputs = runningAuthority.getOutputs();
			outInforesource = outputs[0];

			if (outInforesource == null) {
				info("Inputs inforesource not found");
				return;
			}

			person = getPerson();
			history = getHistory();
			preparation = getPreparation();
			
			infoTreatment();
			drawTestIntarface();
			addInteraction();
			
		}

	}

	public void runProduction(ru.dvo.iacp.is.iacpaas.mas.messages.TaskMessage msg, TaskMessageResultCreator rc) throws PlatformException {
		try {
			MainTest temp = new MainTest();
		} catch (Exception e) {
			info(e.getMessage());
		}
	}
}
