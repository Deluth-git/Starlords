package starlords.util.crossmod;

import com.fs.starfarer.api.util.WeightedRandomPicker;
import second_in_command.SCData;
import second_in_command.SCUtils;
import second_in_command.specs.*;
import starlords.person.Lord;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SCLordsFactory {
    public static void populateExecutiveOfficers(Lord lord) {
        SCData scData = SCUtils.getFleetData(lord.getFleet());

        if (lord.getTemplate().executiveOfficers.size() < 3) {
            /*ArrayList<String> a = new ArrayList<>();
            for (Object b : lord.getTemplate().executiveOfficers.keySet().toArray()){
                a.add((String) b);
            }*/
            WeightedRandomPicker<SCBaseAptitudePlugin> aptitudePicker = fillRandomAptitudePicker(
                    new ArrayList<>(lord.getTemplate().executiveOfficers.keySet()),//lord.getTemplate().executiveOfficers.keySet().stream().toList(),
                    scData,
                    lord
            );
            for (int i = lord.getTemplate().executiveOfficers.size(); i < 3; i++) {
                String aptitudeId = pickRandomAptitude(aptitudePicker);
                lord.getTemplate().executiveOfficers.put(aptitudeId, new ArrayList<>());
            }
        }

        int currentSlot = 0;
        for (String aptitudeId : lord.getTemplate().executiveOfficers.keySet() ) {
            SCOfficer officer = new SCOfficer(lord.getFaction().createRandomPerson(), aptitudeId);
            List<String> lordExecutiveOfficerSkills = lord.getTemplate().executiveOfficers.get(aptitudeId);
            int currentSkill = 0;
            List<String> unlockedSkills = new ArrayList<>();
            for (String skillId : lordExecutiveOfficerSkills) {
                officer.addSkill(skillId);
                unlockedSkills.add(skillId);
                currentSkill++;
            }
            for (int i = currentSkill; i < 5; i++) {
                String newSkill = pickRandomSkill(officer, unlockedSkills);
                if (newSkill != null) {
                    officer.addSkill(newSkill);
                    unlockedSkills.add(newSkill);
                }
            }
            scData.setOfficerInSlot(currentSlot, officer);
            currentSlot++;
        }
    }

    private static WeightedRandomPicker<SCBaseAptitudePlugin> fillRandomAptitudePicker(List<String> unlockedAptitudes, SCData data, Lord lord) {
        WeightedRandomPicker<SCBaseAptitudePlugin> aptitudePicker = new WeightedRandomPicker<>();
        List<SCBaseAptitudePlugin> availableAptitudes = SCSpecStore.getAptitudeSpecs().stream().map(SCAptitudeSpec::getPlugin).collect(Collectors.toList());
        availableAptitudes = availableAptitudes.stream().filter(aptitude -> !unlockedAptitudes.contains(aptitude.getId())).collect(Collectors.toList());
        for (SCBaseAptitudePlugin availableAptitude : availableAptitudes) {
            aptitudePicker.add(availableAptitude, availableAptitude.getNPCFleetSpawnWeight(data, lord.getFleet()) + 0.01f);
        }
        return aptitudePicker;
    }

    private static String pickRandomAptitude(WeightedRandomPicker<SCBaseAptitudePlugin> aptitudePicker) {
        return aptitudePicker.pickAndRemove().getId();
    }

    private static String pickRandomSkill(SCOfficer officer, List<String> unlockedSkills) {
        WeightedRandomPicker<String> unlockableSkills = new WeightedRandomPicker<>();
        SCBaseAptitudePlugin aptitude = officer.getAptitudePlugin();
        //aptitude.clearSections();
        aptitude.createSections();
        List<SCAptitudeSection> sections = aptitude.getSections();
        for (SCAptitudeSection section : sections) {
            for (String skillId : section.getSkills()) {
                if (!unlockedSkills.contains(skillId)) {
                    SCSkillSpec skillSpec = SCSpecStore.getSkillSpec(skillId);
                    if (skillSpec != null) {
                        unlockableSkills.add(skillId, skillSpec.getNpcSpawnWeight() + 0.01f);
                    } else {
                        unlockableSkills.add(skillId);
                    }
                }
            }
        }
        return unlockableSkills.pick();
    }
}
