package com.codeshelf.api.resources.subresources;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.shiro.authz.annotation.RequiresPermissions;

import lombok.Setter;

import com.codeshelf.api.BaseResponse;
import com.codeshelf.api.ParameterUtils;
import com.codeshelf.behavior.TestBehavior;
import com.codeshelf.model.domain.Facility;

public class TestResource {

	@Setter
	private Facility facility;
	
	@Setter
	private TestBehavior testBehavior;

	@POST
	@Path("/{functionName}")
	@RequiresPermissions("che:simulate")
	public Response callFunction(@PathParam("functionName") String functionName, MultivaluedMap<String, String> functionParams) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Method method = TestBehavior.class.getMethod(functionName, new Class<?>[] {Facility.class, Map.class});
		Object response = method.invoke(testBehavior, facility, ParameterUtils.toMapOfFirstValues(functionParams));
		return BaseResponse.buildResponse(response);
	}
	
}
