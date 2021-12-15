package com.github.francescociraolo.trace;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public interface RecordSpecification {

    interface DestinationHolder {
        int getDestinationCpu();
    }

    interface SourceHolder {
        int getSourceCpu();
    }

    class SchedWakeupFamilySpec implements RecordSpecification, DestinationHolder {

        private final ProcessInfo wakingProcess;
        private final int priority;
        private final Boolean success;
        private final int cpu;
        private final Type type;

        private SchedWakeupFamilySpec(Type type, Matcher matcher) {
            this.wakingProcess = new ProcessInfo(Integer.parseInt(matcher.group("pid")),
                    matcher.group("processName"));
            this.priority = Integer.parseInt(matcher.group("num"));
            if (matcher.groupCount() == 6)
                success = null;
            else
                this.success = matcher.group("success").equals("1");
            this.cpu = Integer.parseInt(matcher.group("cpu"));
            this.type = type;
        }

        public ProcessInfo getWakingProcess() {
            return wakingProcess;
        }

        public int getPriority() {
            return priority;
        }

        public Boolean isSuccess() {
            return success;
        }

        public int getDestinationCpu() {
            return cpu;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public boolean isPidRelated(int pid) {
            return getWakingProcess().getPid() == pid;
        }

        @Override
        public boolean isCoreRelated(int core) {
            return cpu == core;
        }

        public static SchedWakeupFamilySpec buildSchedWakeup(Matcher matcher) {
            return new SchedWakeupFamilySpec(Type.SCHED_WAKEUP, matcher);
        }

        public static SchedWakeupFamilySpec buildSchedWakeupNew(Matcher matcher) {
            return new SchedWakeupFamilySpec(Type.SCHED_WAKEUP_NEW, matcher);
        }

        @Override
        public String toString() {
            return String.format("%s [%d] success=%d CPU:%03d",
                    wakingProcess.toTwoPointsString(),
                    priority,
                    success ? 1 : 0,
                    cpu);
        }
    }

    class SchedSwitchSpec implements RecordSpecification {

        private final ProcessInfo previousProcess;
        private final int previousPriority;
        private final char previousState;

        private final ProcessInfo nextProcess;
//        private final int nextPID;
        private final int nextPriority;

        public SchedSwitchSpec(Matcher matcher) {
            this.previousProcess = new ProcessInfo(Integer.parseInt(matcher.group("ppid")), matcher.group("pprocess"));
            this.previousPriority = Integer.parseInt(matcher.group("pprio"));
            this.previousState = matcher.group("pstate").charAt(0);
            this.nextProcess = new ProcessInfo(Integer.parseInt(matcher.group("npid")), matcher.group("nprocess"));
//            this.nextPID = Integer.parseInt(matcher.group("npid"));
            this.nextPriority = Integer.parseInt(matcher.group("nprio"));
        }

        public ProcessInfo getPreviousProcess() {
            return previousProcess;
        }

        public int getPreviousPriority() {
            return previousPriority;
        }

        public char getPreviousState() {
            return previousState;
        }

        public ProcessInfo getNextProcess() {
            return nextProcess;
        }

        public int getNextPriority() {
            return nextPriority;
        }

        @Override
        public Type getType() {
            return Type.SCHED_SWITCH;
        }

        @Override
        public boolean isPidRelated(int pid) {
            return previousProcess.getPid() == pid || nextProcess.getPid() == pid;
        }

        @Override
        public String toString() {
            return String.format("%s [%d] %s ==> %s [%d]", previousProcess.toTwoPointsString(), previousPriority, previousState, nextProcess.toTwoPointsString(), nextPriority);
        }
    }

    class SchedMigrateTaskSpec implements RecordSpecification, DestinationHolder, SourceHolder {

        private final ProcessInfo process;
        private final int priority;
        private final int originCPU;
        private final int destinationCPU;

        private SchedMigrateTaskSpec(Matcher matcher) {
            this.process = new ProcessInfo(Integer.parseInt(matcher.group("pid")), matcher.group("processName"));

            this.priority = Integer.parseInt(matcher.group("prio"));
            this.originCPU = Integer.parseInt(matcher.group("ocpu"));
            this.destinationCPU = Integer.parseInt(matcher.group("dcpu"));
        }

        public ProcessInfo getProcess() {
            return process;
        }

        public String getProcessName() {
            return process.getName();
        }

        public int getPid() {
            return process.getPid();
        }

        public int getPriority() {
            return priority;
        }

        public int getSourceCpu() {
            return originCPU;
        }

        public int getDestinationCpu() {
            return destinationCPU;
        }

        @Override
        public Type getType() {
            return Type.SCHED_MIGRATE_TASK;
        }

        @Override
        public boolean isPidRelated(int pid) {
            return getPid() == pid;
        }

        @Override
        public boolean isCoreRelated(int core) {
            return originCPU == core || destinationCPU == core;
        }

        @Override
        public String toString() {
            return String.format("comm=%s pid=%d prio=%d orig_cpu=%d dest_cpu=%d", getProcessName(), getPid(), priority, originCPU, destinationCPU);
        }
    }

    class FunctionSpec implements RecordSpecification {

        private final String functionName;

        private FunctionSpec(String functionName) {
            this.functionName = functionName;
        }

        public String getFunctionName() {
            return functionName;
        }

        @Override
        public Type getType() {
            return Type.FUNCTION;
        }

        @Override
        public String toString() {
            return functionName;
        }
    }

    class KernelStackSpec implements RecordSpecification {

        private KernelStackSpec() {
        }

        @Override
        public Type getType() {
            return Type.KERNEL_STACK;
        }

        @Override
        public String toString() {
            return "<stack trace >";
        }
    }

    abstract class FunctionGraphSpec implements RecordSpecification {

        protected final int depth;
        protected final Duration duration;

        public FunctionGraphSpec(int depth, Duration duration) {
            this.depth = depth;
            this.duration = duration;
        }

        public int getDepth() {
            return depth;
        }

        public Duration getDuration() {
            return duration;
        }

        public static class Duration {
            private final String duration;
            private final String unit;

            public Duration(String duration, String unit) {
                this.duration = duration;
                this.unit = unit;
            }

            public String getDurationString() {
                return duration;
            }

            public String getUnitString() {
                return unit;
            }

            public static Duration parse(Matcher matcher) {
                var duration = matcher.group("duration");
                var unit = matcher.group("unit");

                Duration res = null;

                if (duration.isBlank()) {
                    if (!unit.isBlank())
                        throw new RuntimeException("Duration is empty by unit is not");
                } else if (unit.isBlank())
                    throw new RuntimeException("Unit is empty by duration is not");
                else
                    res = new Duration(duration, unit);


                return res;
            }

            public static String toString(Duration duration) {
                return duration == null ? "" : String.format("%s %s", duration.duration, duration.unit);
            }
        }
    }

    class FunctionEntrySpec extends FunctionGraphSpec {

        private final String functionName;
        private final boolean closed;

        public FunctionEntrySpec(Matcher matcher) {
            super(matcher.group("depth").length(), Duration.parse(matcher));
            this.functionName = matcher.group("fname");
            this.closed = matcher.group("type").equals(";");
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public Type getType() {
            return Type.FUNCTION_ENTRY;
        }

        public String getFunctionName() {
            return functionName;
        }

        @Override
        public String toString() {
            return String.format("%16s  |%s%s()%s",
                    Duration.toString(duration),
                    " ".repeat(depth),
                    functionName,
                    closed ? ";" : " {");
        }
    }

    class FunctionExitSpec extends FunctionGraphSpec {

        public FunctionExitSpec(Matcher matcher) {
            super(matcher.group("depth").length(), Duration.parse(matcher));
        }

        @Override
        public Type getType() {
            return Type.FUNCTION_EXIT;
        }

        @Override
        public String toString() {
            return String.format("%17s  |%s}",
                    Duration.toString(duration),
                    " ".repeat(depth));
        }
    }

    Type getType();

    default boolean isCoreRelated(int core) {
        return false;
    }

    default boolean isPidRelated(int pid) {
        return false;
    }

    static RecordSpecification parse(String typeString, String message) {
        return Type.findTypeByString(typeString).map(t -> t.parse(message)).orElse(null);
    }


    enum TypeCategory {
        EVENT,
        PLUGIN,
        STACK_TRACE
    }

    enum Type {
        SCHED_WAKEUP("sched_wakeup",
                TypeCategory.EVENT,
                SchedWakeupFamilySpec::buildSchedWakeup,
                "(?<processName>.*):(?<pid>\\d*) \\[(?<num>\\d*)](<CANT FIND FIELD success>| success=(?<success>\\d*)) CPU:(?<cpu>\\d*)",
                SpecificationProperties.DESTINATION_CPU),
        SCHED_WAKEUP_NEW("sched_wakeup_new",
                TypeCategory.EVENT,
                SchedWakeupFamilySpec::buildSchedWakeupNew,
                "(?<processName>.*):(?<pid>\\d*) \\[(?<num>\\d*)](<CANT FIND FIELD success>| success=(?<success>\\d*)) CPU:(?<cpu>\\d*)",
                SpecificationProperties.DESTINATION_CPU),
        SCHED_SWITCH("sched_switch",
                TypeCategory.EVENT,
                SchedSwitchSpec::new,
                "(?<pprocess>.*):(?<ppid>\\d*) \\[(?<pprio>\\d*)] (?<pstate>\\w) ==> (?<nprocess>.*):(?<npid>\\d*) \\[(?<nprio>\\d*)]"),
        SCHED_MIGRATE_TASK("sched_migrate_task",
                TypeCategory.EVENT,
                SchedMigrateTaskSpec::new,
                "comm=(?<processName>.*) pid=(?<pid>\\d*) prio=(?<prio>\\d*) orig_cpu=(?<ocpu>\\d*) dest_cpu=(?<dcpu>\\d*)",
                SpecificationProperties.DESTINATION_CPU,
                SpecificationProperties.SOURCE_CPU),
        FUNCTION("function", TypeCategory.PLUGIN, FunctionSpec::new),
        FUNCTION_ENTRY(
                "funcgraph_entry",
                TypeCategory.PLUGIN,
                FunctionEntrySpec::new,
                "\\s*(?<duration>(\\d*(\\.\\d*)?)?)(?<unit>( \\S*)?)\\s*\\|(?<depth>\\s*)(?<fname>.*)\\(\\)(?<type>( \\{)|;)"
        ),
        FUNCTION_EXIT(
                "funcgraph_exit",
                TypeCategory.PLUGIN,
                FunctionExitSpec::new,
                "\\s*(?<duration>\\d*(\\.\\d*)?)(?<unit> \\S*?)\\s*\\|(?<depth>\\s*)}"
        ),
        KERNEL_STACK("kernel_stack",
                TypeCategory.STACK_TRACE,
                new KernelStackSpec());

        private final String typeString;
        private final TypeCategory category;

        private final RecordSpecification singleton;

        private final Function<Matcher, ? extends RecordSpecification> parser;
        private final Pattern pattern;

        private final Function<String, ? extends RecordSpecification> builder;

        private final Set<SpecificationProperties> properties;


        Type(String typeString,
             TypeCategory category,
             RecordSpecification specification,
             SpecificationProperties... properties) {
            this.typeString = typeString;
            this.category = category;

            this.singleton = specification;

            this.builder = null;
            this.parser = null;
            this.pattern = null;

            this.properties = Set.of(properties);
        }

        Type(String typeString,
             TypeCategory category,
             Function<String, ? extends RecordSpecification> parser,
             SpecificationProperties... properties) {
            this.typeString = typeString;
            this.category = category;
            this.builder = parser;

            this.singleton = null;
            this.parser = null;
            this.pattern = null;

            this.properties = Set.of(properties);
        }

        Type(String typeString,
             TypeCategory category,
             Function<Matcher, ? extends RecordSpecification> parser,
             String pattern,
             SpecificationProperties... properties) {
            this.typeString = typeString;
            this.category = category;
            this.parser = parser;
            this.pattern = Pattern.compile(pattern);

            this.singleton = null;
            this.builder = null;

            this.properties = Set.of(properties);
        }

        public TypeCategory getCategory() {
            return category;
        }

        public String getTypeString() {
            return typeString;
        }

        public RecordSpecification parse(String message) {
            RecordSpecification res = singleton;

            if (builder != null)
                res = builder.apply(message);

            if (parser != null) {
                var matcher = pattern.matcher(message);

                if (!matcher.find())
                    throw new RuntimeException(String.format("Invalid message: %s", message));

                res = parser.apply(matcher);
            }

            return res;
        }

        public boolean hasDestinationCpu() {
            return properties.contains(SpecificationProperties.DESTINATION_CPU);
        }

        public boolean hasSourceCpu() {
            return properties.contains(SpecificationProperties.SOURCE_CPU);
        }

        public boolean isStackTrace() {
            return category == TypeCategory.STACK_TRACE;
        }


        private static final Map<String, Type> typeByString = new TreeMap<>();

        public static Optional<Type> findTypeByString(String typeString) {
            if (typeByString.isEmpty())
                typeByString.putAll(Arrays
                        .stream(Type.values())
                        .collect(Collectors.toMap(Type::getTypeString, Function.identity())));
            return Optional.ofNullable(typeByString.get(typeString));
        }
    }

    enum SpecificationProperties {
        DESTINATION_CPU,
        SOURCE_CPU,
    }
}
