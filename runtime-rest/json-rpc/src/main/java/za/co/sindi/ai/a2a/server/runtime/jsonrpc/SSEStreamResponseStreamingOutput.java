/**
 * 
 */
package za.co.sindi.ai.a2a.server.runtime.jsonrpc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import za.co.sindi.ai.a2a.types.Event;
import za.co.sindi.ai.a2a.types.Message;
import za.co.sindi.ai.a2a.types.RequestId;
import za.co.sindi.ai.a2a.types.SendStreamingMessageSuccessResponse;
import za.co.sindi.ai.a2a.types.Task;
import za.co.sindi.ai.a2a.types.TaskArtifactUpdateEvent;
import za.co.sindi.ai.a2a.types.TaskStatusUpdateEvent;
import za.co.sindi.ai.a2a.utils.JsonUtils;

/**
 * @author Buhake Sindi
 * @since 03 December 2025
 */
public class SSEStreamResponseStreamingOutput implements StreamingOutput {

	private final RequestId id;
	private final Publisher<Event> publisher;
	
	/**
	 * @param id
	 * @param publisher
	 */
	public SSEStreamResponseStreamingOutput(RequestId id, Publisher<Event> publisher) {
		super();
		this.id = Objects.requireNonNull(id, "A JSON-RPC Request Id is required.");
		this.publisher = Objects.requireNonNull(publisher, "A publisher is required.");
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		// TODO Auto-generated method stub
		// Use a BufferedWriter for efficient writing
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
        	// Example using a simple blocking consumer (requires careful backpressure handling in a real app):
            CountDownLatch latch = new CountDownLatch(1);
            publisher.subscribe(new Subscriber<Event>() {
                private Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                    (this.subscription = s).request(Long.MAX_VALUE); // Request all data at once (simple, but potentially problematic for large streams)
                }

                @Override
                public void onNext(Event event) {
                    try {
                    	SendStreamingMessageSuccessResponse response = null;
                    	if (event instanceof Message message) response = new SendStreamingMessageSuccessResponse(id, message);
                    	else if (event instanceof Task task) response = new SendStreamingMessageSuccessResponse(id, task);
                    	else if (event instanceof TaskStatusUpdateEvent statusUpdate) response = new SendStreamingMessageSuccessResponse(id, statusUpdate);
                    	else if (event instanceof TaskArtifactUpdateEvent artifactUpdate) response = new SendStreamingMessageSuccessResponse(id, artifactUpdate);
                    	
                    	writer.write("data: " + JsonUtils.marshall(response) + "\n\n");
                        writer.flush(); // Flush immediately to ensure data is streamed
                    } catch (IOException e) {
                        onError(e); // Propagate IO errors
                    }
                    // For manual backpressure, you would call subscription.request(n) here
                }

                @Override
                public void onError(Throwable t) {
                    // Handle error (e.g., log it or rethrow as a WebApplicationException)
                    latch.countDown();
                    throw new RuntimeException("Error in reactive stream", t);
                }

                @Override
                public void onComplete() {
                    latch.countDown(); // Signal completion
                }
            });

            // Wait for the stream to complete
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Streaming interrupted", e);
            }
        } catch (Exception e) {
            throw new WebApplicationException("Failed to stream data.", e);
        }
	}
}

