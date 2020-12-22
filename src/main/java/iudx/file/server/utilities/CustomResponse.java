package iudx.file.server.utilities;

import io.vertx.core.json.JsonObject;

public class CustomResponse {

	private final int statusCode;
	private final Object message;
	private final String httpMesage;
	
	private CustomResponse(int statusCode, String message, String httpMessage) {
		this.statusCode = statusCode;
		this.message = message;
		this.httpMesage = httpMessage;
	}

	public JsonObject toJson() {
		JsonObject json = new JsonObject();
		json.put("statusCode", statusCode);
		json.put("message", message);
		json.put("metadata", httpMesage);
		return json;
	}
	
	public String toJsonString() {
		return toJson().toString();
	}
	

	public static class ResponseBuilder {
		private int statusCode;
		private String message;
		private String httpMessage;

		public ResponseBuilder() {
		}

		public ResponseBuilder withStatusCode(int code) {
			this.statusCode = code;
			return this;
		}

		public ResponseBuilder withMessage(String message) {
			this.message = message;
			return this;
		}
		
		public ResponseBuilder withCustomMessage(String message) {
			this.httpMessage=message;
			return this;
		}
		
		public CustomResponse build() {
			return new CustomResponse(statusCode, message, httpMessage);
		}

	}

}
