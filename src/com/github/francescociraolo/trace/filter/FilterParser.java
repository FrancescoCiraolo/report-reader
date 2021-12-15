package com.github.francescociraolo.trace.filter;

import com.github.francescociraolo.trace.Record;
import com.github.francescociraolo.trace.RecordSpecification;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilterParser {

    private interface FilterConstant {

        Predicate<Record> parse(TermOperator operator, String... right);
    }

    private static final Map<String, FilterConstant> constants = Map.of(
            "p", (operator, right) -> {
                switch (operator) {
                    case LT:
                    case GT:
                    case LTE:
                    case GTE:
                        throw new RuntimeException("Invalid operator " + operator + " for p constant");
                    case EQ:
                        var pid = Integer.parseInt(right[0]);
                        return record -> record.getProcessPid() == pid;
                    case IN:
                        var set = Arrays
                                .stream(right)
                                .map(Integer::parseInt)
                                .collect(Collectors.toSet());
                        return record -> set.contains(record.getProcessPid());
                    default:
                        throw new RuntimeException();
                }
            },
            "sp", (operator, right) -> {
                switch (operator) {
                    case LT:
                    case GT:
                    case LTE:
                    case GTE:
                        throw new RuntimeException("Invalid operator " + operator + " for sp constant");
                    case EQ:
                        var pid = Integer.parseInt(right[0]);
                        return record -> record.getSpecification().isPidRelated(pid);
//                        return record -> record.getPid() == pid || record.getSpecification().isPidRelated(pid);
                    case IN:
                        var set = Arrays
                                .stream(right)
                                .map(Integer::parseInt)
                                .collect(Collectors.toSet());
                        return record -> {
                            boolean found = false;
                            Iterator<Integer> iterator = set.iterator();
                            while (!found && iterator.hasNext())
                                found = record.getSpecification().isPidRelated(iterator.next());
                            return found;
                        };
                    default:
                        throw new RuntimeException();
                }
            },
            "c", (operator, right) -> {
                switch (operator) {
                    case LT:
                    case GT:
                    case LTE:
                    case GTE:
                        throw new RuntimeException("Invalid operator " + operator + " for c constant");
                    case EQ:
                        var core = Integer.parseInt(right[0]);
                        return record -> record.getCore() == core;
                    case IN:
                        var set = Arrays
                                .stream(right)
                                .map(Integer::parseInt)
                                .collect(Collectors.toSet());
                        return record -> set.contains(record.getCore());
                    default:
                        throw new RuntimeException();
                }
            },
            "sc", (operator, right) -> {
                switch (operator) {
                    case LT:
                    case GT:
                    case LTE:
                    case GTE:
                        throw new RuntimeException("Invalid operator " + operator + " for sc constant");
                    case EQ:
                        var core = Integer.parseInt(right[0]);
                        return record -> record.getSpecification().isCoreRelated(core);
//                        return record -> record.getCore() == core || record.getSpecification().isCoreRelated(core);
                    case IN:
                        var set = Arrays
                                .stream(right)
                                .map(Integer::parseInt)
                                .collect(Collectors.toSet());
                        return record -> {
                            boolean found = false;
                            Iterator<Integer> iterator = set.iterator();
                            while (!found && iterator.hasNext())
                                found = record.getSpecification().isCoreRelated(iterator.next());
                            return found;
                        };
//                        return record -> set.contains(record.getCore()) || record.getSpecification().hasCoreRelated(set);
                    default:
                        throw new RuntimeException();
                }
            },
            "t", (operator, right) -> {
                var v = Double.parseDouble(right[0]);
                switch (operator) {
                    case LT:
                        return record -> record.getTimestamp().compareTo(v) < 0;
                    case GT:
                        return record -> record.getTimestamp().compareTo(v) > 0;
                    case LTE:
                        return record -> record.getTimestamp().compareTo(v) <= 0;
                    case GTE:
                        return record -> record.getTimestamp().compareTo(v) >= 0;
                    case EQ:
                        return record -> record.getTimestamp().compareTo(v) == 0;
                    case IN:
                        throw new RuntimeException("Invalid operator " + operator + " for t constant");
                    default:
                        throw new RuntimeException();
                }
            },
            "type", (operator, right) -> {
                switch (operator) {
                    case LT:
                    case GT:
                    case LTE:
                    case GTE:
                        throw new RuntimeException("Invalid operator " + operator + " for type constant");

                    case EQ:
                        var type = RecordSpecification.Type.findTypeByString(right[0]).orElseThrow();
                        return record -> record.getSpecification().getType() == type;

                    case IN:
                        var set = Arrays
                                .stream(right)
                                .map(RecordSpecification.Type::findTypeByString)
                                .map(Optional::orElseThrow)
                                .collect(Collectors.toSet());

                        return record -> set.contains(record.getSpecification().getType());

                    default:
                        throw new RuntimeException();
                }
            }
    );

    enum TermOperator {
        EQ,
        GT,
        LT,
        GTE,
        LTE,
        IN
    }

    enum Operator {
        AND,
        OR
    }

    private final char[] filterString;
    private int pos = 0;

    private FilterParser(String filterString) {
        this.filterString = filterString.toCharArray();

    }

    private RecordFilter parseString() {
        var predicate = expr();

        return new RecordFilter("Expression filter", new String(filterString)) {
            @Override
            public boolean test(Record record) {
                return predicate.test(record);
            }
        };
    }

    private void strip() {
        //noinspection StatementWithEmptyBody
        for (; pos < filterString.length && filterString[pos] == ' '; pos++) ;
    }

    private TermOperator termOperator() {
        strip();
        TermOperator op;
        switch (filterString[pos++]) {
            case '=':
                op = TermOperator.EQ;
//                opValid(pos);
                break;
            case '>':
                op = TermOperator.GT;
                if (filterString[pos] == '=') {
                    op = TermOperator.GTE;
                    pos++;
                }
//                opValid(pos);
                break;
            case '<':
                op = TermOperator.LT;
                if (filterString[pos] == '=') {
                    op = TermOperator.LTE;
                    pos++;
                }
//                opValid(pos);
                break;
            case '/':
                op = TermOperator.IN;
//                opValid(pos);
                break;
            default:
                throw new RuntimeException("INVALID OPERATOR");
        }
        return op;
    }

    private Operator exprOperator() {
        strip();
        switch (filterString[pos++]) {
            case '|':
                return Operator.OR;
            case '&':
                return Operator.AND;
            default:
                throw new RuntimeException("Unknown operator " + filterString[pos - 1]);
        }
    }

    private String fact() {
        strip();

        var builder = new StringBuilder();

        for (; pos < filterString.length && (Character.isLetterOrDigit(filterString[pos]) || filterString[pos] == '.' || filterString[pos] == '_'); pos++)
            builder.append(filterString[pos]);

        return builder.toString();
    }

    private Predicate<Record> term() {
        Predicate<Record> predicate;
        strip();

        if (filterString[pos] == '(') {
            pos++;
            predicate = expr();
            if (filterString[pos++] != ')')
                throw new RuntimeException("Unclosed brackets");
        } else {
            var constant = constants.get(fact());
            var op = termOperator();

            if (op == TermOperator.IN) {
                strip();
                if (filterString[pos] != '[') throw new RuntimeException("After IN is required a [list]");
                strip();
                var list = new LinkedList<String>();
                while (filterString[pos++] != ']') {
                    list.add(fact());
                    strip();
                    if (filterString[pos] != ',' & filterString[pos] != ']')
                        throw new RuntimeException("Lists have to be comma separated");
                }
                predicate = constant.parse(op, list.toArray(String[]::new));
            } else {
                var right = fact();
                predicate = constant.parse(op, right);
            }
        }

        return predicate;
    }

    private Predicate<Record> expr() {
        strip();
        Predicate<Record> p = term();

        for (; pos < filterString.length && filterString[pos] != ')'; ) {
            strip();
            var operator = exprOperator();
            var expr = term();
            switch (operator) {
                case AND:
                    p = p.and(expr);
                    break;
                case OR:
                    p = p.or(expr);
                    break;
            }
        }

        return p;
    }

    public static RecordFilter parse(String filterString) {
        return new FilterParser(filterString).parseString();
    }
}
