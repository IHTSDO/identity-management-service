package org.snomed.ims.domain.crowd;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;

/**
 * Control serialisation of User object. Sensitive information is omitted by default (through purpose lack of writing
 * values).
 */
public class UserView extends JsonSerializer<User> {
	@Override
	public void serialize(User user, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
		if (user == null || jsonGenerator == null || serializerProvider == null) {
			throw new IllegalArgumentException("656efb0d-d826-41ff-ad69-8b5d8a553e40 Cannot serialise User");
		}

		jsonGenerator.writeStartObject();
		withPropertyNullable(jsonGenerator, "login", user.getLogin());
		withPropertyNullable(jsonGenerator, "firstName", user.getFirstName());
		withPropertyNullable(jsonGenerator, "lastName", user.getLastName());
		withPropertyNullable(jsonGenerator, "email", user.getEmail());
		withPropertyNullable(jsonGenerator, "displayName", user.getDisplayName());
		withPropertyNullable(jsonGenerator, "active", user.isActive());
		withPropertyNullable(jsonGenerator, "username", user.getLogin()); // Backwards compatible
		withPropertyNullable(jsonGenerator, "roles", user.getRoles());
		jsonGenerator.writeEndObject();
	}

	protected void withPropertyNullable(JsonGenerator jsonGenerator, String key, String value) throws IOException {
		if (value != null && !value.isEmpty()) {
			jsonGenerator.writeStringField(key, value);
		}
	}

	protected void withPropertyNullable(JsonGenerator jsonGenerator, String key, Boolean value) throws IOException {
		if (value != null) {
			jsonGenerator.writeBooleanField(key, value);
		}
	}

	protected void withPropertyNullable(JsonGenerator jsonGenerator, String key, List<String> value) throws IOException {
		if (value != null && !value.isEmpty()) {
			jsonGenerator.writeFieldName(key);
			jsonGenerator.writeStartArray();
			for (String v : value) {
				jsonGenerator.writeString(v);
			}
			jsonGenerator.writeEndArray();
		}
	}
}
