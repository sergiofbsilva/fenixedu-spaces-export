package org.fenixedu.spaces.migration;

import java.util.ArrayList;
import java.util.List;

import net.sourceforge.fenixedu.domain.Person;
import net.sourceforge.fenixedu.domain.resource.ResourceAllocation;
import net.sourceforge.fenixedu.domain.resource.ResourceResponsibility;
import net.sourceforge.fenixedu.domain.space.PersonSpaceOccupation;
import net.sourceforge.fenixedu.domain.space.Space;
import net.sourceforge.fenixedu.domain.space.SpaceResponsibility;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic.TxMode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ExportPartySpaceOccupationTask extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    private static String dealWithDates(YearMonthDay yearMonthDay) {
        return yearMonthDay == null ? null : yearMonthDay.toString("dd/MM/yyyy");
    }

    public static class PersonOccupationBean {
        public String personUsername;
        public String spaceId;
        public String start;
        public String end;

        public PersonOccupationBean(String personUsername, String spaceId, String start, String end) {
            super();
            this.personUsername = personUsername;
            this.spaceId = spaceId;
            this.start = start;
            this.end = end;
        }
    }

    public static class SpaceResponsibilityBean {
        public String partyId;
        public String spaceId;
        public String start;
        public String end;

        public SpaceResponsibilityBean(String partyId, String spaceId, String start, String end) {
            super();
            this.partyId = partyId;
            this.spaceId = spaceId;
            this.start = start;
            this.end = end;
        }

    }

    @Override
    public void runTask() throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        output("space_occupations.json", gson.toJson(processPersonSpaceOccupations()).getBytes());
        output("person_occupations.json", gson.toJson(processSpaceResponsabilityOccupations()).getBytes());
    }

    private List<SpaceResponsibilityBean> processSpaceResponsabilityOccupations() {
        List<SpaceResponsibilityBean> beans = new ArrayList<>();

        for (ResourceResponsibility resourceResponsability : Bennu.getInstance().getResourceResponsibilitySet()) {
            if (resourceResponsability.isSpaceResponsibility()) {
                SpaceResponsibility spaceResposibility = (SpaceResponsibility) resourceResponsability;
                String partyId = spaceResposibility.getParty().getExternalId();
                String spaceId = spaceResposibility.getSpace().getExternalId();

                String start = dealWithDates(spaceResposibility.getBegin());
                String end = dealWithDates(spaceResposibility.getEnd());

                beans.add(new SpaceResponsibilityBean(partyId, spaceId, start, end));
            }
        }
        return beans;
    }

    private List<PersonOccupationBean> processPersonSpaceOccupations() {
        List<PersonOccupationBean> personOccupations = new ArrayList<>();
        for (ResourceAllocation resourceAllocation : Bennu.getInstance().getResourceAllocationsSet()) {
            if (resourceAllocation.isPersonSpaceOccupation()) {
                PersonSpaceOccupation pso = (PersonSpaceOccupation) resourceAllocation;

                Person person = pso.getPerson();
                Space space = pso.getSpace();

                YearMonthDay start = pso.getBegin();
                YearMonthDay end = pso.getEnd();

                personOccupations.add(new PersonOccupationBean(person.getIstUsername(), space.getExternalId(),
                        dealWithDates(start), dealWithDates(end)));

            }
        }
        return personOccupations;
    }

}
