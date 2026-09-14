package org.snomed.ims.domain;

import org.springframework.boot.jackson.ObjectValueSerializer;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;

import java.util.List;

/**
 * Control serialisation of User object. Sensitive information is omitted by default (through purpose lack of writing
 * values).
 */
public class UserView extends ObjectValueSerializer<User> {

	@Override
	protected void serializeObject(User user, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
		withPropertyNullable(jsonGenerator, "login", user.getLogin());
		withPropertyNullable(jsonGenerator, "firstName", user.getFirstName());
		withPropertyNullable(jsonGenerator, "lastName", user.getLastName());
		withPropertyNullable(jsonGenerator, "email", user.getEmail());
		withPropertyNullable(jsonGenerator, "displayName", user.getDisplayName());
		withPropertyNullable(jsonGenerator, "active", user.isActive());
		withPropertyNullable(jsonGenerator, "username", user.getLogin()); // Backwards compatible
		withPropertyNullable(jsonGenerator, "roles", user.getRoles());
		withPropertyNullable(jsonGenerator, "clientAccess", user.getClientAccess());
	}

	protected void withPropertyNullable(JsonGenerator jsonGenerator, String key, String value) {
		if (value != null && !value.isEmpty()) {
			jsonGenerator.writeStringProperty(key, value);
		}
	}

	protected void withPropertyNullable(JsonGenerator jsonGenerator, String key, Boolean value) {
		if (value != null) {
			jsonGenerator.writeBooleanProperty(key, value);
		}
	}

	protected void withPropertyNullable(JsonGenerator jsonGenerator, String key, List<String> value) {
		if (value != null && !value.isEmpty()) {
			jsonGenerator.writeName(key);
			jsonGenerator.writeStartArray();
			for (String v : value) {
				jsonGenerator.writeString(v);
			}
			jsonGenerator.writeEndArray();
		}
	}
}
