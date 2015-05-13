package ru.dvo.iacp.is.iacpaas.mas.agents;

import java.util.ArrayList;

import ru.dvo.iacp.is.iacpaas.common.exceptions.PlatformException;
import ru.dvo.iacp.is.iacpaas.mas.IRunningAuthority;
import ru.dvo.iacp.is.iacpaas.storage.IConcept;
import ru.dvo.iacp.is.iacpaas.storage.IInforesource;
import ru.dvo.iacp.is.iacpaas.storage.exceptions.StorageException;
import ru.dvo.iacp.is.iacpaas.storage.generator.IConceptGenerator;
import ru.dvo.iacp.is.iacpaas.storage.generator.IInforesourceGenerator;

public final class AgentFiltrImpl extends AgentFiltr {

	private AgentFiltrImpl link = this;
	private IInforesource inforesource;
	private IInforesourceGenerator generator;
	
	public AgentFiltrImpl(IRunningAuthority runningAuthority, IInforesource agentInforesource) {
		super(runningAuthority, agentInforesource);
	}
	
	public class FarmList {
		
		private static final String �ONTRAIND = "����������������";
		private static final String OTHER_DRUGS = "�������������� � ������� ��";
		private static final String DRUGS = "��";
		private static final String AGE = "������ �������";
		
		private IConcept drug;
		private IConcept[] allConceptsFarmList;
		private IConcept[] children;
		
		private IConcept getDrug(String name) throws StorageException {
			
			for (int i = 0; i < children.length; i++) {
				if (name.equals(children[i].getName())) {
					return children[i];
				}
			}
			return null;
		}
		
		public String[] getContraind(String name) throws StorageException {
			
			drug = getDrug(name);
			
			if ((drug != null) && (drug.hasRelation(�ONTRAIND))) {
				IConcept[] child = drug.gotoByMeta(�ONTRAIND).getChildren();
				ArrayList<String> list = new ArrayList<String>();
				
				for (int i = 0; i < child.length; i++) {
					String value = drug.gotoByMeta(�ONTRAIND).getChildren()[i].getValue();
					list.add(value);
				}
				return list.toArray(new String[list.size()]);
			}
			return null;
		}

		public boolean getInteraction(String drugs) throws StorageException {
			
			for (int i = 0; i < children.length; i++) {
				drug = children[i];
				if (drug.hasRelation(OTHER_DRUGS)) {
					int counter = drug.gotoByMeta(OTHER_DRUGS).getChildren().length;

					for (int j = 0; j < counter; j++) {
						IConcept conceptOther = drug.gotoByMeta(OTHER_DRUGS).getChildren()[j];
						String value = conceptOther.getChildren()[0].getValue();
					
						if (value.equals(drugs)) {
							return false;
						}
					}
				}
			}
			return true;
		}
		
		public double getAge(String name) throws StorageException {
			
			drug = getDrug(name);
			
			if ((drug != null) && (drug.hasRelation(AGE)) && (drug.getName().equals(name))) {
				double age = drug.gotoByMeta(AGE).getValue();
				return age;
			}
			return 0;
		}
		
		public FarmList() throws StorageException {		
			IInforesource[] inputs = runningAuthority.getInputs();
			IInforesource infoFarmList = inputs[0];
			
			if(infoFarmList == null) {
				info("Inputs inforesource not found");
				return;
			}
			
			allConceptsFarmList = infoFarmList.getAllConcepts();
			children = allConceptsFarmList[0].nextSetByMeta(DRUGS); //��������������
		}
		
	}
	
	public class Buffer {
		
		private static final String ROOT = "�����������������";
		private static final String INFO_HISTORY = "���� �� ��";
		private static final String RECOMMENDED = "�������������";
		private static final String DEFLECTABLE = "�����������";
		private static final String WARNING = "��������������";
		private static final String INCOMPATIBLE = "������������� ����";
		private static final String DIAGNOSIS = "�������";
		private static final String DRUG = "��";
		private static final String FEATURE = "�����������";
		private static final String AGE = "�������";
		
		private IConceptGenerator root;
		private IConceptGenerator deflectable;
		private IConceptGenerator warning;
		private IConcept[] recommended;
		
		private double age;
		private String diagnosis;
		private String[] feature;
		
		private FarmList farmList = new FarmList();
		
		private void setInfoPerson() throws StorageException {
			
			IConceptGenerator mapHistory = (IConceptGenerator) root.gotoByMeta(INFO_HISTORY);	
			ArrayList<String> list = new ArrayList<String>();
			
			diagnosis = mapHistory.gotoByMeta(DIAGNOSIS).getValue();
			age = mapHistory.gotoByMeta(AGE).getValue();
			IConcept[] isFeature = mapHistory.nextSetByMeta(FEATURE);
			
			for (int i = 0; i < isFeature.length; i++) {
				String value = isFeature[i].getValue();
				list.add(value);
			}
			feature = list.toArray(new String[list.size()]);
		}
		
		private void filterAge() throws StorageException {

			recommended = root.gotoByMeta(RECOMMENDED).getChildren();
			deflectable = (IConceptGenerator) root.gotoByMeta(DEFLECTABLE);
			double ageDrug = 0;
			
			for (int i = 0; i < recommended.length; i++) {
				ageDrug = farmList.getAge(recommended[i].getName());
				
				if (!(ageDrug <= age)) {
					deflectable.generateWithName(DRUG, recommended[i].getName());
					recommended[i].delete(link);
				} 
			}
		}

		private void filterFeature() throws StorageException {
			
			recommended = root.gotoByMeta(RECOMMENDED).getChildren();
			
			label: for (int i = 0; i < recommended.length; i++) {
					   String[] contraind = farmList.getContraind(recommended[i].getName());
	
					   if (contraind == null) {
						   continue;
					   }
					
					   for (int j = 0; j < contraind.length; j++) {
						   for (int k = 0; k < feature.length; k++) {
							
							   if (contraind[j].equals(feature[k])) {
							  	   warning.generateWithName(DRUG, recommended[i].getName());
								   recommended[i].delete(link);
								   continue label; 
							   }
						   }
					   }
					}
		}
		
		public Buffer() throws StorageException {
			
			root = (IConceptGenerator) generator.generateFromAxiom().gotoByMeta(ROOT);
			
			setInfoPerson();
			filterAge();
			filterFeature();
			
		}
		
	}
	
	public void runProduction(ru.dvo.iacp.is.iacpaas.mas.messages.TaskMessage msg, TaskMessageResultCreator rc) throws PlatformException {
		
		try {
			inforesource = msg.getAgentTestReportInforesource();
			generator = inforesource.getGenerator(this);
			
			Buffer buffer = new Buffer();
		} catch (Exception e) {
			info(e.getMessage());
		}
		
	}
	
}

