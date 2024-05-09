package com.microsoft.semantickernel.sample.java.sk.assistant.routes;

import com.azure.core.annotation.BodyParam;
import com.microsoft.semantickernel.orchestration.FunctionResult;
import com.microsoft.semantickernel.sample.java.sk.assistant.KernelType;
import com.microsoft.semantickernel.sample.java.sk.assistant.SemanticKernelProvider;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.PlayerController;
import com.microsoft.semantickernel.sample.java.sk.assistant.datastore.Players;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.Player;
import com.microsoft.semantickernel.sample.java.sk.assistant.models.StatementType;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionArguments;
import com.microsoft.semantickernel.semanticfunctions.KernelFunctionMetadata;
import com.microsoft.semantickernel.services.audio.AudioContent;
import com.microsoft.semantickernel.services.audio.AudioToTextExecutionSettings;
import com.microsoft.semantickernel.services.audio.TextToAudioExecutionSettings;
import com.microsoft.semantickernel.services.audio.TextToAudioService;
import com.microsoft.semantickernel.services.chatcompletion.AuthorRole;
import com.microsoft.semantickernel.services.chatcompletion.ChatHistory;
import com.microsoft.semantickernel.services.chatcompletion.ChatMessageContent;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang3.ArrayUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides the path for the main assistant endpoint for receiving user instructions.
 */
@Path("/assistant")
public class Assistant {
    // TODO: Allow user to select in the UI which player id audio instructions are for
    public static final String DEFAULT_PLAYER_ID = "8b39317e-ffca-4a9c-a100-5adc65047de3";

    private final Players players;
    private final SemanticKernelProvider semanticKernelProvider;
    private final PlayerController playerController;
    private final ChatHistory chatHistory = new ChatHistory(false);
    private ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();

    @Inject
    Assistant(
            SemanticKernelProvider semanticKernelProvider,
            PlayerController playerController,
            Players players) {
        this.players = players;
        this.semanticKernelProvider = semanticKernelProvider;
        this.playerController = playerController;
    }

    public Mono<byte[]> getAudio(String sampleText) {

        TextToAudioService service = semanticKernelProvider
                .getTextToAudioService();

        // Set execution settings (optional)
        TextToAudioExecutionSettings executionSettings = TextToAudioExecutionSettings.builder()
                .withVoice("echo")
                .withResponseFormat("wav")
                .withSpeed(1.0)
                .build();

        // Convert text to audio
        return service.getAudioContentAsync(
                        sampleText,
                        executionSettings)
                .map(AudioContent::getData);
    }


    public record ResultWithAudio(String result, List<Byte> audio) {
    }

    @POST
    @Path("upload/audio")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<ResultWithAudio> uploadAudio(
            @FormParam("type")
            String type,
            @FormParam("id")
            int id,
            @FormParam("recording")
            boolean stillRecording,
            @FormParam("audio_data")
            File filename
    ) throws IOException {

        synchronized (audioBuffer) {
            byte[] recording = Files.readAllBytes(filename.toPath());

            audioBuffer.writeBytes(recording);
            System.out.println("TYPE: " + type + " " + recording.length + " " + id + " " + audioBuffer.size());

            // upload when the recording stops, or every 60 seconds
            if (id > 0 && (id % 6) == 0 || !stillRecording) {
                recording = audioBuffer.toByteArray();
                audioBuffer.reset();

                String names = String.join(", ", players.getPlayerNames());

                String prompt = """
                        This recording is of a an RPG player speaking, it might include the following names: %s.
                        It might also include silence, return nothing if nothing is said.
                        """
                        .formatted(names)
                        .stripIndent();

                Mono<String> text = semanticKernelProvider
                        .getAudioToTextService()
                        .getTextContentsAsync(
                                AudioContent.builder()
                                        .withData(recording)
                                        .withModelId("whisper-1")
                                        .build(),
                                AudioToTextExecutionSettings.builder()
                                        .withFilename(filename.getName() + "." + type)
                                        .withPrompt(prompt)
                                        .build()
                        )
                        .flatMap(this::getNewActions);


                Mono<ResultWithAudio> result = text
                        .flatMap(actions -> {
                            return describeActions(actions)
                                    .map(bytes -> {
                                        List<Byte> data = Arrays.asList(ArrayUtils.toObject(bytes));

                                        try(FileOutputStream fos = new FileOutputStream("/tmp/recording.mp3")) {
                                            fos.write(bytes);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        return new ResultWithAudio(actions, data);
                                    });
                        });


                return Uni.createFrom().future(result.toFuture());
            }
        }
        return Uni.createFrom().item(new ResultWithAudio("", new ArrayList<>()));
    }

    private Mono<byte[]> describeActions(String actions) {
        return semanticKernelProvider
                .getKernel(KernelType.DESCRIBE_ACTIONS)
                .flatMap(kernel -> {
                    return kernel.getFunction("Language", "SummarizeActionsTaken")
                            .invokeAsync(kernel)
                            .withArguments(KernelFunctionArguments
                                    .builder()
                                    .withVariable("history", chatHistory)
                                    .withVariable("actions", actions)
                                    .build())
                            .withResultType(String.class);
                })
                .flatMap(summary -> {
                    return getAudio(summary.getResult());
                });
    }

    private Mono<String> getNewActions(String dialogue) {
        return semanticKernelProvider
                .getKernel(KernelType.DESCRIBE_ACTIONS)
                .flatMap(kernel -> {
                    return kernel.getFunction("Language", "ExtractActions")
                            .invokeAsync(kernel)
                            .withArguments(KernelFunctionArguments
                                    .builder()
                                    .withVariable("history", chatHistory)
                                    .withVariable("dialogue", dialogue)
                                    .build())
                            .withResultType(String.class);
                })
                .flatMapMany(result -> {
                    return Flux.fromStream(Arrays.stream(result.getResult().split("\\n")));
                })
                .filter(s -> !"NONE".equalsIgnoreCase(s))
                .map(action -> new ChatMessageContent(
                        AuthorRole.ASSISTANT,
                        action
                ))
                .collectList()
                .flatMapMany(messages -> {

                    chatHistory.addMessage(new ChatMessageContent(
                            AuthorRole.SYSTEM,
                            "ACTIONS:"
                    ));

                    List<ChatMessageContent<?>> chatMessages = new ArrayList(messages);
                    chatHistory.addAll(new ChatHistory(chatMessages));

                    return Flux.fromIterable(messages)
                            .flatMap(m -> {
                                return playerController.buildPlan(
                                                players.getPlayer(DEFAULT_PLAYER_ID),
                                                m.getContent(),
                                                false
                                        )
                                        .map(result -> {
                                            return "EVENTS: " + m.getContent() + "\n    ACTION RESULT: " + result;
                                        });
                            });
                })
                .collectList()
                .map(messages -> String.join("\n", messages));
    }

    @POST
    @Path("perform/{playerId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> performRequest(
            @PathParam("playerId")
            String playerId,

            @BodyParam("statement")
            String statement
    ) {
        Player player = players.getPlayer(playerId);
        if (player == null) {
            return Uni.createFrom().failure(new NotFoundException("Customer not found"));
        }

        String cleaned = statement.replaceAll("^[^a-zA-Z0-9]+", "").trim();

        CompletableFuture<String> future =
                classifyStatement(cleaned)
                        .flatMap(type -> {
                            switch (type) {
                                case ACTION, REQUEST, EVENT -> {
                                    boolean externalResourcePlan = false;
                                    if (statement.startsWith("/")) {
                                        externalResourcePlan = true;
                                    }

                                    return playerController.buildPlan(player, cleaned, externalResourcePlan)
                                            .switchIfEmpty(Mono.just("Done"));
                                }
                                case QUESTION -> {
                                    return playerController.answerQuestion(player, cleaned);
                                }
                                case FACT -> {
                                    return playerController.addPlayerFact(player, cleaned)
                                            .then(Mono.just("Recorded note: statement"));
                                }
                                default -> {
                                    return Mono.just("Unknown statement type");
                                }
                            }
                        })
                        .toFuture();

        return Uni.createFrom().future(future);
    }



    public Mono<StatementType> classifyStatement(String statement) {
        return semanticKernelProvider.getKernel(KernelType.QUERY)
                .flatMap(kernel -> {
                    return kernel
                            .invokeAsync("Language", "StatementType")
                            .withArguments(
                                    KernelFunctionArguments.builder()
                                            .withInput(statement)
                                            .build()
                            )
                            .withResultType(String.class);
                })
                .map(FunctionResult::getResult)
                .map(String::toUpperCase)
                .map(String::strip)
                .map(StatementType::valueOf);
    }
}
