/* * Copyright 2014 http://Bither.net * * Licensed under the Apache License, Version 2.0 (the "License"); * you may not use this file except in compliance with the License. * You may obtain a copy of the License at * *    http://www.apache.org/licenses/LICENSE-2.0 * * Unless required by applicable law or agreed to in writing, software * distributed under the License is distributed on an "AS IS" BASIS, * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. * See the License for the specific language governing permissions and * limitations under the License. */package net.bither.http;/** * http statusCode is not 200 ,internal_server_error:500, bad_gateway:502 * service_unavailable:503 *  * @author jjz *  */public class HttpServerException extends HttpException {	/**	 * 	 */	private static final long serialVersionUID = 1L;	public HttpServerException(String msg, int statusCode) {		super(msg, statusCode);	}	public HttpServerException(String msg, Exception cause, int statusCode) {		super(msg, cause, statusCode);	}	public HttpServerException(String msg, Exception cause) {		super(msg, cause);	}	public HttpServerException(String msg) {		super(msg);	}	public HttpServerException(Exception cause) {		super(cause);	}}