package dev.vfyjxf.moddev.runtime.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

final class BrigadierCommandSupport {

    private static final int MAX_CAPTURED_MESSAGES = 20;

    private BrigadierCommandSupport() {
    }

    static CommandListResult list(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack ignoredSource,
            CommandType type,
            CommandQuery query
    ) {
        var filter = query.query() == null ? "" : query.query().trim().toLowerCase(Locale.ROOT);
        var limit = Math.max(1, query.limit());
        var matches = dispatcher.getRoot().getChildren().stream()
                .filter(LiteralCommandNode.class::isInstance)
                .map(LiteralCommandNode.class::cast)
                .map(node -> describeNode(node, type))
                .filter(descriptor -> matches(descriptor, filter))
                .sorted(java.util.Comparator.comparing(CommandDescriptor::name))
                .toList();
        var truncated = matches.size() > limit;
        var commands = truncated ? matches.subList(0, limit) : matches;
        return new CommandListResult(commands, matches.size(), truncated);
    }

    static CommandSuggestionResult suggest(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source,
            CommandSuggestionQuery query
    ) {
        var normalizedInput = normalize(query.input());
        var cursor = Math.max(0, Math.min(query.cursor(), normalizedInput.length()));
        ParseResults<CommandSourceStack> parseResults = dispatcher.parse(normalizedInput, source);
        try {
            var suggestions = dispatcher.getCompletionSuggestions(parseResults, cursor).get();
            var items = suggestions.getList().stream()
                    .limit(Math.max(1, query.limit()))
                    .map(suggestion -> new CommandSuggestion(
                            suggestion.getText(),
                            suggestion.getRange().getStart(),
                            suggestion.getRange().getEnd(),
                            suggestion.getTooltip() == null ? "" : suggestion.getTooltip().getString()
                    ))
                    .toList();
            return new CommandSuggestionResult(normalizedInput, parseResults.getReader().getCursor(), items);
        } catch (Exception exception) {
            throw new CommandServiceException("command_runtime_unavailable", exception.getMessage());
        }
    }

    static CommandExecutionResult execute(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source,
            CommandExecutionRequest request
    ) {
        var normalizedCommand = normalize(request.command());
        if (normalizedCommand.isBlank()) {
            return CommandExecutionResult.failure(normalizedCommand, "command_parse_error", "Command must not be blank", List.of());
        }
        if (!rootExists(dispatcher, normalizedCommand)) {
            return CommandExecutionResult.failure(normalizedCommand, "command_not_found", "Unknown command root", List.of());
        }

        var collector = new MessageCollector();
        var resultCode = new AtomicInteger();
        var callbackTriggered = new AtomicInteger();
        var instrumentedSource = source
                .withSource(collector)
                .withCallback((success, result) -> {
                    callbackTriggered.incrementAndGet();
                    resultCode.set(result);
                });
        var parseResults = dispatcher.parse(normalizedCommand, instrumentedSource);
        var parseException = Commands.getParseException(parseResults);
        if (parseException != null) {
            return CommandExecutionResult.failure(
                    normalizedCommand,
                    "command_parse_error",
                    parseException.getMessage(),
                    collector.messages()
            );
        }
        try {
            var executeResult = dispatcher.execute(parseResults);
            if (callbackTriggered.get() == 0) {
                resultCode.set(executeResult);
            }
            return CommandExecutionResult.success(normalizedCommand, resultCode.get(), collector.messages());
        } catch (Exception exception) {
            return CommandExecutionResult.failure(
                    normalizedCommand,
                    "command_execution_failed",
                    exception.getMessage(),
                    collector.messages()
            );
        }
    }

    static String normalize(String input) {
        if (input == null) {
            return "";
        }
        var normalized = input.trim();
        return normalized.startsWith("/") ? normalized.substring(1).trim() : normalized;
    }

    private static boolean matches(CommandDescriptor descriptor, String filter) {
        if (filter.isBlank()) {
            return true;
        }
        return descriptor.name().toLowerCase(Locale.ROOT).contains(filter)
                || descriptor.usage().toLowerCase(Locale.ROOT).contains(filter)
                || descriptor.summary().toLowerCase(Locale.ROOT).contains(filter);
    }

    private static CommandDescriptor describeNode(LiteralCommandNode<CommandSourceStack> node, CommandType type) {
        var origin = inferOrigin(node);
        var summary = switch (origin.source()) {
            case "minecraft" -> "Built-in Minecraft command";
            case "mod" -> "Mod-provided command";
            default -> "Registered runtime command";
        };
        return new CommandDescriptor(
                node.getName(),
                "/" + node.getUsageText(),
                origin.source(),
                origin.namespace(),
                summary,
                type
        );
    }

    private static Origin inferOrigin(CommandNode<CommandSourceStack> node) {
        var queue = new ArrayDeque<CommandNode<CommandSourceStack>>();
        queue.add(node);
        while (!queue.isEmpty()) {
            var current = queue.removeFirst();
            var command = current.getCommand();
            if (command != null) {
                var className = command.getClass().getName();
                if (className.startsWith("net.minecraft.")) {
                    return new Origin("minecraft", "minecraft");
                }
                if (className.startsWith("net.neoforged.")) {
                    return new Origin("mod", "neoforge");
                }
                return new Origin("mod", namespaceFromClassName(className));
            }
            queue.addAll(current.getChildren());
        }
        return new Origin("unknown", "");
    }

    private static String namespaceFromClassName(String className) {
        var packageName = className.contains(".") ? className.substring(0, className.lastIndexOf('.')) : className;
        if (packageName.startsWith("java.") || packageName.startsWith("com.mojang.")) {
            return "";
        }
        var segments = packageName.split("\\.");
        return segments.length == 0 ? "" : segments[segments.length - 1];
    }

    private static boolean rootExists(CommandDispatcher<CommandSourceStack> dispatcher, String normalizedCommand) {
        var firstToken = normalizedCommand.split("\\s+", 2)[0];
        return dispatcher.getRoot().getChild(firstToken) != null;
    }

    private record Origin(String source, String namespace) {
    }

    private static final class MessageCollector implements CommandSource {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component component) {
            if (messages.size() >= MAX_CAPTURED_MESSAGES) {
                return;
            }
            messages.add(component.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        public List<String> messages() {
            return List.copyOf(messages);
        }
    }
}

