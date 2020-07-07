package com.abruno.jiraautomation;
import static io.restassured.RestAssured.*;
import static io.restassured.RestAssured.given;

import java.io.File;

import org.testng.Assert;


import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.path.json.JsonPath;

public class JiraWebservicesTest {
	public static void main(String[] args) {
		
		//Add comment in Jira
		RestAssured.baseURI="http://localhost:8080";
		
		//Login scenario
		SessionFilter session = new SessionFilter();
		
		String expectedMessage = "This is an expected message";
		
		String response = given().relaxedHTTPSValidation()
				.header("Content-Type","application/json")
				.body("{ \"username\": \"someusername\", \"password\": \"somepassword\"}")
		.log().all()
		.filter(session)
		.when()
		.post("/rest/auth/1/session")
		.then()
		.log().all()
		.extract().response().asString();
		
		
		String addCommentResponse = given().pathParam("id", "RES-17").log().all().header("Content-Type","application/json")
		.body("{\r\n" + 
				"    \"body\": \""+expectedMessage+"\r\n" + 
				"}")
		.filter(session)
		.when()
		.post("/rest/api/2/issue/{id}/comment")
		.then()
		.log().all()
		.statusCode(201).extract().response().asString();
		JsonPath jp1 = new JsonPath(addCommentResponse);
		String commentId = jp1.getString("id");
		
		//Add attachment
		given().header("X-Atlassian-Token", "no-check").filter(session).pathParam("id", "RES-17")
		.queryParam("fields", "comment")
		.multiPart("file", new File("jira.txt"))
		.when()
		.post("rest/api/2/issue/{id}/attachments")
		.then().log().all().assertThat().statusCode(200); 
		
		//Get issue
		String issueDetails = given().filter(session).pathParam("key", "RES-17")
		.when().get("rest/api/2/issue/{id}")
		.then().log().all().extract().response().asString();
		System.out.println(issueDetails);
	
		JsonPath jp2 = new JsonPath(issueDetails);
		int commentsCount = jp2.getInt("fields.comments.comments.size()");
		
		for(int i = 0; i > commentsCount; i++) {
			String commentIdIssue = jp2.get("fields.comments.comments["+i+"].id");
			if (commentIdIssue.equalsIgnoreCase(commentId)) {
				String message = jp1.get("fields.comments.comments[\"+i+\"].body").toString();
				System.out.println(message);
				Assert.assertEquals(message, expectedMessage);
			}
		}
		
	}
}
