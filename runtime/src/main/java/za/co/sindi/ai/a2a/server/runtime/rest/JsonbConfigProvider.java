package za.co.sindi.ai.a2a.server.runtime.rest;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import za.co.sindi.ai.a2a.utils.json.JsonAPISchemeInAdapter;
import za.co.sindi.ai.a2a.utils.json.JsonJSONRPCVersionAdapter;
import za.co.sindi.ai.a2a.utils.json.JsonMessageRoleAdapter;
import za.co.sindi.ai.a2a.utils.json.JsonTaskStateAdapter;
import za.co.sindi.ai.a2a.utils.json.JsonTransportProtocolAdapter;

/**
 * @author Buhake Sindi
 * @since 25 March 2025
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JsonbConfigProvider implements ContextResolver<Jsonb> {

	private final Jsonb jsonb;
	
	/**
	 * 
	 */
	public JsonbConfigProvider() {
		super();
		//TODO Auto-generated constructor stub
		JsonbConfig config = new JsonbConfig()
					.withAdapters(new JsonAPISchemeInAdapter(),
						new JsonJSONRPCVersionAdapter(),
						new JsonMessageRoleAdapter(),
						new JsonTaskStateAdapter(),
						new JsonTransportProtocolAdapter());
		this.jsonb = JsonbBuilder.create(config);
	}

	/* (non-Javadoc)
	 * @see jakarta.ws.rs.ext.ContextResolver#getContext(java.lang.Class)
	 */
	@Override
	public Jsonb getContext(Class<?> type) {
		// TODO Auto-generated method stub
		return jsonb;
	}
}
