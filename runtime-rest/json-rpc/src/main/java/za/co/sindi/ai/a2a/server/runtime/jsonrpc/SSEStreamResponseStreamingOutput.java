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
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.StreamingOutput;
import za.co.sindi.ai.a2a.server.A2AServerError;
import za.co.sindi.ai.a2a.types.SendStreamingMessageSuccessResponse;
import za.co.sindi.ai.a2a.utils.JsonUtils;

/**
 * @author Buhake Sindi
 * @since 03 December 2025
 */
public class SSEStreamResponseStreamingOutput implements StreamingOutput {

	private static final Logger LOGGER = Logger.getLogger(SSEStreamResponseStreamingOutput.class.getName());
	private final Publisher<SendStreamingMessageSuccessResponse> publisher;
	
	/**
	 * @param publisher
	 */
	public SSEStreamResponseStreamingOutput(Publisher<SendStreamingMessageSuccessResponse> publisher) {
		super();
		this.publisher = Objects.requireNonNull(publisher, "A publisher is required.");
	}

	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		// TODO Auto-generated method stub
		// Use a BufferedWriter for efficient writing
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
        	// Example using a simple blocking consumer (requires careful backpressure handling in a real app):
            publisher.subscribe(new Subscriber<>() {

                @Override
                public void onSubscribe(Subscription subscription) {}

                @Override
                public void onNext(SendStreamingMessageSuccessResponse response) {
                    try {
                    	writer.write("data: " + JsonUtils.marshall(response) + "\n\n");
                        writer.flush(); // Flush immediately to ensure data is streamed
                    } catch (IOException e) {
                        onError(e); // Propagate IO errors
                    }
                }

                @Override
                public void onError(Throwable t) {
                    // Handle error (e.g., log it or rethrow as a WebApplicationException)
                	LOGGER.log(Level.SEVERE, "Error encountered from reactive stream.", t);
                	if (t instanceof A2AServerError e) throw e;
                	throw new A2AServerError(new za.co.sindi.ai.a2a.types.InternalError(t.getMessage()));
                }

                @Override
                public void onComplete() {
                	LOGGER.info("Reactive streaming completed.");
                }
            });
        } catch (Exception e) {
            throw new WebApplicationException("Failed to stream data.", e);
        }
	}
}
