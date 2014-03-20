package com.gadgetworks.codeshelf.ws.command.req;

import static com.gadgetworks.codeshelf.util.TestUtil.toDoubleQuote;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import org.junit.Assert;

import static com.natpryce.makeiteasy.MakeItEasy.a;
import static com.natpryce.makeiteasy.MakeItEasy.make;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.gadgetworks.codeshelf.model.PositionTypeEnum;
import com.gadgetworks.codeshelf.model.dao.IDaoProvider;
import com.gadgetworks.codeshelf.model.domain.DAOMaker;
import com.gadgetworks.codeshelf.model.domain.DAOTestABC;
import com.gadgetworks.codeshelf.model.domain.Facility;
import com.gadgetworks.codeshelf.model.domain.Path;
import com.gadgetworks.codeshelf.model.domain.Path.PathDao;
import com.gadgetworks.codeshelf.model.domain.PathSegment;
import com.gadgetworks.codeshelf.model.domain.PathSegment.PathSegmentDao;
import com.gadgetworks.codeshelf.model.domain.Point;
import com.gadgetworks.codeshelf.model.domain.WorkArea;
import com.gadgetworks.codeshelf.model.domain.WorkArea.WorkAreaDao;
import com.gadgetworks.codeshelf.ws.command.resp.IWsRespCmd;


@RunWith(MockitoJUnitRunner.class)
public class CreatePathReqTest extends DAOTestABC {

	@Mock
	private IDaoProvider mockDaoProvider;
	
	@Test
	public void testShouldCreatePath() throws JsonParseException, JsonMappingException, IOException {
		int numberOfSegments = 3;
		String testPathDomainId = "DOMID";
		
		DAOMaker maker = new DAOMaker(mSchemaManager);
		Facility testFacility = make(a(maker.TestFacility));
		Path.DAO = new PathDao(mSchemaManager);
		PathSegment.DAO = new PathSegmentDao(mSchemaManager);
		WorkArea.DAO = new WorkAreaDao(mSchemaManager);
	
		Path noPath = Path.DAO.findByDomainId(testFacility, testPathDomainId);
		Assert.assertNull(noPath);

		
		when(mockDaoProvider.getDaoInstance(Facility.class)).thenReturn(Facility.DAO);
		
		ObjectNode mockJsonNode = createReqCmdJsonNode(testFacility.getPersistentId(), testPathDomainId, numberOfSegments);
		
		ObjectMethodWsReqCmd subject = new ObjectMethodWsReqCmd("commandId", mockJsonNode, mockDaoProvider);
		IWsRespCmd respCmd = subject.exec();
		Assert.assertFalse(respCmd.isError());
		Path createdPath1 = Path.DAO.findByDomainId(testFacility, testPathDomainId);
		Assert.assertNotNull(createdPath1);
		Assert.assertEquals(numberOfSegments, createdPath1.getSegments().size());
	}
	
	private ObjectNode createReqCmdJsonNode(UUID facilityId, String pathDomainId, int numberOfSegments) throws JsonParseException, JsonMappingException, IOException {
		
		ObjectMapper mapper = new ObjectMapper();

		ArrayNode segmentArray = mapper.createArrayNode();
		for (int i = 0; i < numberOfSegments; i++) {
			segmentArray.add(createPathSegmentNode("P.", i, 1*i,1*i,1*i,1*i, 1*i, 1*i));
		}
		
		ArrayNode methodArgs = mapper.createArrayNode();
		methodArgs.addAll(Arrays.asList(
			(JsonNode)createMethodArg("pathDomainId", new TextNode(pathDomainId), String.class),
			(JsonNode)createMethodArg("segments", segmentArray, PathSegment[].class)
		));
		
		ObjectNode objectNode = mapper.createObjectNode();
		objectNode.put(IWsReqCmd.CLASSNAME, "com.gadgetworks.codeshelf.model.domain.Facility");
		objectNode.put(IWsReqCmd.PERSISTENT_ID, facilityId.toString());
		objectNode.put(IWsReqCmd.METHODNAME, "createPath");
		objectNode.put(IWsReqCmd.METHODARGS, methodArgs);
			
		return objectNode;
	}

	private ObjectNode createMethodArg(String name, JsonNode value, Class<?> javaType) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode = mapper.createObjectNode();
		objectNode.put("name", name);
		objectNode.put("value", value);
		objectNode.put("classType", javaType.getName());
		return objectNode;
	}

	private ObjectNode createPathSegmentNode(String domainPrefix, int segmentOrder, double startPosX, double startPosY, double startPosZ, double endPosX, double endPosY, double endPosZ) {
		
		
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode = mapper.createObjectNode();
		objectNode.put("segmentOrder", segmentOrder);
		objectNode.put("domainId", domainPrefix + segmentOrder);
		objectNode.put("segmentOrder", segmentOrder);
		objectNode.put("posTypeEnum", PositionTypeEnum.METERS_FROM_PARENT.toString());
		objectNode.put("startPosX", startPosX);
		objectNode.put("startPosY", startPosY);
		objectNode.put("startPosZ", startPosZ);
		objectNode.put("endPosX", endPosX);
		objectNode.put("endPosY", endPosY);	
		objectNode.put("endPosZ", endPosZ);
		return objectNode;
	}
	
	
	private Point point(double x, double y, double z) {
		Point point = new Point(PositionTypeEnum.METERS_FROM_PARENT, x, y, z);
		return point;
	}
}
