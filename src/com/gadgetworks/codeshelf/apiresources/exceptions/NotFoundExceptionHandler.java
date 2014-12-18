package com.gadgetworks.codeshelf.apiresources.exceptions;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.gadgetworks.codeshelf.apiresources.BaseResponse;
import com.sun.jersey.api.NotFoundException;

@Provider
public class NotFoundExceptionHandler implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException ex) {
        return BaseResponse.buildResponse(HttpServletResponse.SC_NOT_FOUND);
    }
}