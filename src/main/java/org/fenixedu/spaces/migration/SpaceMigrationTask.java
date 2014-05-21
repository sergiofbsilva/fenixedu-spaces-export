package org.fenixedu.spaces.migration;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import net.sourceforge.fenixedu.domain.GenericEvent;
import net.sourceforge.fenixedu.domain.Person;
import net.sourceforge.fenixedu.domain.PunctualRoomsOccupationRequest;
import net.sourceforge.fenixedu.domain.resource.ResourceAllocation;
import net.sourceforge.fenixedu.domain.space.AllocatableSpace;
import net.sourceforge.fenixedu.domain.space.Blueprint;
import net.sourceforge.fenixedu.domain.space.BuildingInformation;
import net.sourceforge.fenixedu.domain.space.CampusInformation;
import net.sourceforge.fenixedu.domain.space.FloorInformation;
import net.sourceforge.fenixedu.domain.space.GenericEventSpaceOccupation;
import net.sourceforge.fenixedu.domain.space.RoomClassification;
import net.sourceforge.fenixedu.domain.space.RoomInformation;
import net.sourceforge.fenixedu.domain.space.RoomSubdivisionInformation;
import net.sourceforge.fenixedu.domain.space.Space;
import net.sourceforge.fenixedu.domain.space.SpaceInformation;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.Interval;
import org.joda.time.YearMonthDay;

import pt.ist.fenixframework.Atomic.TxMode;
import pt.utl.ist.fenix.tools.util.i18n.Language;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@SuppressWarnings("unused")
public class SpaceMigrationTask extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    private static String dealWithDates(YearMonthDay yearMonthDay) {
        return yearMonthDay == null ? null : yearMonthDay.toString("dd/MM/yyyy");
    }

    private class SpaceBean {

        public String parentExternalId;
        public String externalId;
        public String createdOn;
        public Integer examCapacity;
        public Integer normalCapacity;
        public String type;
        public String occupationGroup;
        public String managementSpaceGroup;
        private String lessonOccupationsAccessGroup;
        private String writtenEvaluationOccupationsAccessGroup;
        public Set<SpaceInformationBean> informations;
        public Set<BlueprintBean> blueprints;

        private class BlueprintBean {
            public String validFrom;
            public String validUntil;
            public String creationPerson;
            public String raw;

            public BlueprintBean(Blueprint blueprint) {
                this.validFrom = dealWithDates(blueprint.getValidFrom());
                this.validUntil = dealWithDates(blueprint.getValidUntil());
                this.creationPerson = dealWithPerson(blueprint.getCreationPerson());
                this.raw = BaseEncoding.base64().encode(blueprint.getBlueprintFile().getContent());
            }

            private String dealWithPerson(Person creationPerson) {
                if (creationPerson == null) {
                    return null;
                }
                if (creationPerson.getUser() == null) {
                    return null;
                }
                return creationPerson.getUser().getUsername();
            }
        }

        private class SpaceInformationBean {
            public Integer capacity;
            public String blueprintNumber;
            public String validFrom;
            public String validUntil;
            public String emails;
            public Boolean ageQuality;
            public BigDecimal area;
            public String description;
            public Boolean distanceFromSanitaryInstalationsQuality;
            public String doorNumber;
            public Boolean heightQuality;
            public String identification;
            public Boolean illuminationQuality;
            public String observations;
            public Boolean securityQuality;
            public String classificationCode;
            public String name;
            public String level;

            public SpaceInformationBean(SpaceInformation info) {
                super();
                this.capacity = info.getCapacity();
                this.blueprintNumber = info.getBlueprintNumber();
                this.validFrom = dealWithDates(info.getValidFrom());
                this.validUntil = dealWithDates(info.getValidUntil());
                this.emails = info.getEmails();
                if (info instanceof RoomInformation) {
                    this.ageQuality = ((RoomInformation) info).getAgeQuality();
                    this.area = ((RoomInformation) info).getArea();
                    this.description = ((RoomInformation) info).getDescription();
                    this.distanceFromSanitaryInstalationsQuality =
                            ((RoomInformation) info).getDistanceFromSanitaryInstalationsQuality();
                    this.doorNumber = ((RoomInformation) info).getDoorNumber();
                    this.heightQuality = ((RoomInformation) info).getHeightQuality();
                    this.identification = ((RoomInformation) info).getIdentification();
                    this.illuminationQuality = ((RoomInformation) info).getIlluminationQuality();
                    this.observations = ((RoomInformation) info).getObservations();
                    this.securityQuality = ((RoomInformation) info).getSecurityQuality();
                    final RoomClassification roomClassification = info.getRoomClassification();
                    this.classificationCode = roomClassification != null ? roomClassification.getAbsoluteCode() : null;
                    this.name = ((RoomInformation) info).getIdentification();
                }

                if (info instanceof CampusInformation) {
                    this.name = ((CampusInformation) info).getName();
                }

                if (info instanceof FloorInformation) {
                    this.name = ((FloorInformation) info).getLevel().toString();
                    this.level = this.name;
                }

                if (info instanceof BuildingInformation) {
                    this.name = ((BuildingInformation) info).getName();
                }

                if (info instanceof RoomSubdivisionInformation) {
                    this.name = ((RoomSubdivisionInformation) info).getIdentification();
                }

            }

        }

        public SpaceBean(Space space) {
            super();
            final Space suroundingSpace = space.getSuroundingSpace();
            if (suroundingSpace != null) {
                this.parentExternalId = suroundingSpace.getExternalId();
            }

//            final Group genericEventOccupationsAccessGroup = space.getGenericEventOccupationsAccessGroup();
//            this.occupationGroup =
//                    genericEventOccupationsAccessGroup == null ? null : genericEventOccupationsAccessGroup.convert()
//                            .getExternalId();
//            final Group spaceManagementAccessGroup = space.getSpaceManagementAccessGroup();
//            this.managementSpaceGroup =
//                    spaceManagementAccessGroup == null ? null : spaceManagementAccessGroup.convert().getExternalId();

//              To be used after translator
//            final BennuGroupBridge genericEventOccupationsAccessGroup =
//                    (BennuGroupBridge) space.getGenericEventOccupationsAccessGroup();
//            this.occupationGroup = getGroupExpression(genericEventOccupationsAccessGroup);
//
//            final BennuGroupBridge spaceManagementAccessGroup = (BennuGroupBridge) space.getSpaceManagementAccessGroup();
//            this.managementSpaceGroup = getGroupExpression(spaceManagementAccessGroup);
//
//            final BennuGroupBridge lessonOccupationsAccessGroup = (BennuGroupBridge) space.getLessonOccupationsAccessGroup();
//            this.lessonOccupationsAccessGroup = getGroupExpression(lessonOccupationsAccessGroup);
//
//            final BennuGroupBridge writtenEvaluationOccupationsAccessGroup =
//                    (BennuGroupBridge) space.getWrittenEvaluationOccupationsAccessGroup();
//            this.writtenEvaluationOccupationsAccessGroup = getGroupExpression(writtenEvaluationOccupationsAccessGroup);

//          To be used after translator
//            final Group genericEventOccupationsAccessGroup = space.getGenericEventOccupationsAccessGroup();
//            this.occupationGroup = getGroupExpression(genericEventOccupationsAccessGroup);
//
//            final Group spaceManagementAccessGroup = space.getSpaceManagementAccessGroup();
//            this.managementSpaceGroup = getGroupExpression(spaceManagementAccessGroup);
//
//            final Group lessonOccupationsAccessGroup = space.getLessonOccupationsAccessGroup();
//            this.lessonOccupationsAccessGroup = getGroupExpression(lessonOccupationsAccessGroup);
//
//            final Group writtenEvaluationOccupationsAccessGroup = space.getWrittenEvaluationOccupationsAccessGroup();
//            this.writtenEvaluationOccupationsAccessGroup = getGroupExpression(writtenEvaluationOccupationsAccessGroup);

            this.externalId = space.getExternalId();
            this.createdOn = dealWithDates(space.getCreatedOn());
            this.examCapacity = space.getExamCapacity();
            this.normalCapacity = space.getNormalCapacity();
            this.type = space.getClass().getSimpleName();
            this.informations = new HashSet<>();
            for (SpaceInformation info : space.getSpaceInformationsSet()) {
                this.informations.add(new SpaceInformationBean(info));
            }
            this.blueprints = new HashSet<>();
            for (Blueprint blueprint : space.getBlueprintsSet()) {
                this.blueprints.add(new BlueprintBean(blueprint));
            }
        }

//        public String getGroupExpression(final Group group) {
//            return group == null ? null : group.toPersistentGroup().getExternalId();
//        }
    }

    private static class IntervalBean {
        public String start;
        public String end;

        public IntervalBean(String start, String end) {
            super();
            this.start = start;
            this.end = end;
        }

    }

    private static class EventSpaceOccupationBean {
        public String eventSpaceOccupation;
        public String space;

        public EventSpaceOccupationBean(String eventSpaceOccupationExternalId, String space) {
            super();
            this.eventSpaceOccupation = eventSpaceOccupationExternalId;
            this.space = space;
        }
    }

    private static class OccupationBean {

        public String description;
        public String title;
        public String frequency;
        public String beginDate;
        public String endDate;
        public String beginTime;
        public String endTime;
        public Boolean saturday;
        public Boolean sunday;
        public Set<IntervalBean> intervals;
        public Set<String> spaces;
        public String request;

        public OccupationBean(String description, String title, String frequency, String beginDate, String endDate,
                String beginTime, String endTime, Boolean saturday, Boolean sunday, Set<String> spaces,
                Set<IntervalBean> intervals, String request) {
            super();
            this.description = description;
            this.title = title;
            this.frequency = frequency;
            this.beginDate = beginDate;
            this.endDate = endDate;
            this.beginTime = beginTime;
            this.endTime = endTime;
            this.saturday = saturday;
            this.sunday = sunday;
            this.spaces = spaces;
            this.intervals = intervals;
            this.request = request;
        }

    }

    private Set<EventSpaceOccupationBean> getEventSpaceOccupations() {
        taskLog("total occupations %d\n", Bennu.getInstance().getResourceAllocationsSet().size());
        final Set<EventSpaceOccupationBean> eventSpaceOccupations = new HashSet<>();

        for (ResourceAllocation resourceAllocation : Bennu.getInstance().getResourceAllocationsSet()) {
            if (resourceAllocation.isWrittenEvaluationSpaceOccupation() || resourceAllocation.isLessonInstanceSpaceOccupation()
                    || resourceAllocation.isLessonSpaceOccupation()) {
                eventSpaceOccupations.add(new EventSpaceOccupationBean(resourceAllocation.getExternalId(), resourceAllocation
                        .getResource().getExternalId()));
            }
        }
        return eventSpaceOccupations;
    }

    private Set<OccupationBean> getOccupations() {
        int i = 0;
        taskLog("total occupations %d\n", Bennu.getInstance().getResourceAllocationsSet().size());
        final Set<OccupationBean> eventsOccupations = new HashSet<>();

        for (ResourceAllocation resourceAllocation : Bennu.getInstance().getResourceAllocationsSet()) {
            if (resourceAllocation.isGenericEventSpaceOccupation()) {
                GenericEventSpaceOccupation occupation = (GenericEventSpaceOccupation) resourceAllocation;
                final GenericEvent genericEvent = occupation.getGenericEvent();
                if (genericEvent != null) {
                    String description = genericEvent.getDescription().getContent(Language.pt);
                    String title = genericEvent.getTitle().getContent(Language.pt);
                    String frequency = genericEvent.getFrequency() == null ? null : genericEvent.getFrequency().name();
                    String beginDate = dealWithDates(genericEvent.getBeginDate());
                    String endDate = dealWithDates(genericEvent.getEndDate());
                    String beginTime = genericEvent.getStartTimeDateHourMinuteSecond().toString("HH:mm:ss");
                    String endTime = genericEvent.getEndTimeDateHourMinuteSecond().toString("HH:mm:ss");
                    Boolean saturday = genericEvent.getDailyFrequencyMarkSaturday();
                    Boolean sunday = genericEvent.getDailyFrequencyMarkSunday();
                    Set<String> spaces = new HashSet<>();
                    Set<IntervalBean> intervals = new HashSet<>();
                    for (AllocatableSpace space : genericEvent.getAssociatedRooms()) {
                        spaces.add(space.getExternalId());
                    }

                    for (Interval interval : occupation
                            .getEventSpaceOccupationIntervals((YearMonthDay) null, (YearMonthDay) null)) {
                        final String start = interval.getStart().toString("dd/MM/yyyy HH:mm:ss");
                        final String end = interval.getEnd().toString("dd/MM/yyyy HH:mm:ss");
                        intervals.add(new IntervalBean(start, end));
                    }

                    Set<String> requests = new HashSet<>();
                    PunctualRoomsOccupationRequest request = genericEvent.getPunctualRoomsOccupationRequest();

                    eventsOccupations.add(new OccupationBean(description, title, frequency, beginDate, endDate, beginTime,
                            endTime, saturday, sunday, spaces, intervals, request == null ? null : request.getExternalId()));
                    if (i++ % 100 == 0) {
                        taskLog("processing occupation %d\n", i);
                    }

                }
            }
        }
        return eventsOccupations;
    }

    @Override
    public void runTask() throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
//        dumpSpaces(gson);
//        dumpClassifications(gson);
        dumpOccupations(gson);
//        dumpEventSpaceOccupations(gson);
    }

    private static class ClassificationBean {
        public String name;
        public Set<ClassificationBean> childs;
        public Integer code;

        public ClassificationBean(Integer code, String name, Set<ClassificationBean> childs) {
            super();
            this.code = code;
            this.name = name;
            this.childs = childs;
        }

        public ClassificationBean(Integer code, String name) {
            this(code, name, Sets.<ClassificationBean> newHashSet());
        }
    }

    public void dumpClassifications(Gson gson) {
        Set<ClassificationBean> classificationBeans = new HashSet<>();
        taskLog("Dumping classifications to json ...");

        for (RoomClassification roomClassification : Bennu.getInstance().getRoomClassificationSet()) {
            if (roomClassification.getParentRoomClassification() == null) {
                Set<ClassificationBean> children = new HashSet<ClassificationBean>();
                for (RoomClassification child : roomClassification.getChildRoomClassificationsSet()) {
                    children.add(new ClassificationBean(child.getCode(), child.getName().getContent(Language.pt)));
                }
                ClassificationBean classificationBean =
                        new ClassificationBean(roomClassification.getCode(),
                                roomClassification.getName().getContent(Language.pt), children);
                classificationBeans.add(classificationBean);
            }
        }

        output("classifications.json", gson.toJson(classificationBeans).getBytes());
    }

    public void dumpOccupations(Gson gson) {
        taskLog("Dumping occupations to json ...");
        output("occupations.json", gson.toJson(getOccupations()).getBytes());
        taskLog("Done!");
    }

    public void dumpEventSpaceOccupations(Gson gson) {
        taskLog("Dumping occupations to json ...");
        output("event_space_occupations.json", gson.toJson(getEventSpaceOccupations()).getBytes());
        taskLog("Done!");
    }

    public void dumpSpaces(Gson gson) {
        final Multimap<Space, SpaceInformation> informations = HashMultimap.create();
        for (SpaceInformation information : Bennu.getInstance().getSpaceInformationsSet()) {
            informations.put(information.getSpace(), information);
        }

        taskLog("total spaces %d\n", informations.keySet().size());

        final String json = gson.toJson(FluentIterable.from(informations.keySet()).transform(new Function<Space, SpaceBean>() {
            private int i = 0;

            @Override
            public SpaceBean apply(Space space) {
                if (i++ % 100 == 0) {
                    taskLog("processing space %s\n", i);
                }
                return new SpaceBean(space);
            }

        }).toSet());

        output("spaces.json", json.getBytes());
    }
}