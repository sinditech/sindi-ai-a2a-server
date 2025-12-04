/**
 * 
 */
package za.co.sindi.ai.a2a.server.runtime.rest;

import jakarta.json.bind.annotation.JsonbProperty;
import za.co.sindi.ai.a2a.types.Message;
import za.co.sindi.ai.a2a.types.Task;
import za.co.sindi.ai.a2a.types.TaskArtifactUpdateEvent;
import za.co.sindi.ai.a2a.types.TaskStatusUpdateEvent;

/**
 * @author Buhake Sindi
 * @since 03 December 2025
 */
public record StreamResponse(@JsonbProperty Message message, @JsonbProperty Task task, @JsonbProperty TaskStatusUpdateEvent statusUpdate, @JsonbProperty TaskArtifactUpdateEvent artifactUpdate) {

	public StreamResponse {
		if (message == null && task == null && statusUpdate == null && artifactUpdate == null) {
			throw new IllegalStateException("A stream response requires a streaming kind.");
		}
	}
	
	public StreamResponse(final Message message) {
		this(message, null, null, null);
	}
	
	public StreamResponse(final Task task) {
		this(null, task, null, null);
	}
	
	public StreamResponse(final TaskStatusUpdateEvent statusUpdate) {
		this(null, null, statusUpdate, null);
	}
	
	public StreamResponse(final TaskArtifactUpdateEvent artifactUpdate) {
		this(null, null, null, artifactUpdate);
	}
}
