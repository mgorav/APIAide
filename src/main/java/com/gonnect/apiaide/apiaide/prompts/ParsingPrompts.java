package com.gonnect.apiaide.apiaide.prompts;

public class ParsingPrompts {

    public static final String CODE_PARSING_SCHEMA_TEMPLATE = """
            Here is an API response schema from an OAS and a query.  
            The API's response will follow the schema and be a JSON.  
            Assume you are given a JSON response which is stored in a python dict variable called 'data', 
            your task is to generate Python code to extract information I need from the API response.
            Note: I will give you 'data', do not make up one, just reference it in your code. 
            Please print the final result as brief as possible. If the result is a list, just print it in one sentence. Do not print each item in a new line.
            The example result format are: 
            "The release date of the album is 2002-11-03" 
            "The id of the person is 12345"
            "The movies directed by Wong Kar-Wai are In the Mood for Love (843), My Blueberry Nights (1989), Chungking Express (11104)"
            Note you should generate only Python code.  
            DO NOT use fields that are not in the response schema.   
                    
            API: {api_path}
            API description: {api_description}
            Parameters or body for this API call: 
            {api_param}
                    
            Response JSON schema defined in the OAS:
            {response_schema}
                    
            Example:  
            {response_example}
                    
            The response is about: {response_description}
                    
            Query: {query}
                    
            Begin!    
            Python Code:
            """;

    public static final String CODE_PARSING_RESPONSE_TEMPLATE = """ 
            Here is an API response JSON snippet with its corresponding schema and a query.
            The API's response JSON follows the schema. 
            Assume the JSON response is stored in a python dict variable called 'data', your task is to generate 
            Python code to extract information I need from the API response.
            Please print the final result.
            The example result format are:  
            "The release date of the album is 2002-11-03"
            "The id of the person is 12345" 
            Note you should generate only Python code.
            DO NOT use fields that are not in the response schema.
                    
            API: {api_path}
            API description: {api_description}
            Parameters for this API call:
            {api_param}
                    
            Response JSON schema defined in the OAS: 
            {response_schema}
                    
            JSON snippet:  
            {json}
                    
            Query: {query}
                    
            Python Code:
            """;

    public static final String LLM_PARSING_TEMPLATE = """
            Here is an API JSON response with its corresponding API description:
                    
            API: {api_path}  
            API description: {api_description}
            Parameters for this API call: 
            {api_param}
                    
            JSON response: 
            {json}
                    
            The response is about: {response_description}
                    
            ====
            Your task is to extract some information according to these instructions: {query} 
            When working with API objects, you should usually use ids over names.  
            If the response indicates an error, you should instead output a summary of the error.  
                    
            Output:
            """;

    public static final String LLM_SUMMARIZE_TEMPLATE = """
            Here is an API JSON response with its corresponding API description:
                    
            API: {api_path}
            API description: {api_description}
            Parameters for this API call:
            {api_param}
                    
            JSON response:
            {json}
                    
            The response is about: {response_description}
                    
            ====
            Your task is to extract some information according to these instructions: {query}
            If the response does not contain the needed information, you should translate the response JSON into natural language. 
            If the response indicates an error, you should instead output a summary of the error.
                    
            Output: 
            """;

    public static final String CODE_PARSING_EXAMPLE_TEMPLATE = """
            Here is an API response schema and a query.  
            The API's response will follow the schema and be a JSON.   
            Assume you are given a JSON response which is stored in a python dict variable called 'data', your task is to generate Python code to extract information I need from the API response. 
            Please print the final result.
            The example result format are:
            Note you should generate only Python code.
            DO NOT use fields that are not in the response schema. 
                    
            API: {api_path}
            API description: {api_description}
                    
            Response example: 
            {response_example}
                    
            Query: {query}
                    
            Python Code:
            """;

    public static final String POSTPROCESS_TEMPLATE = """
            Given a string, due to the maximum context length, the final item/sentence may be truncated and incomplete. 
            First, remove the final truncated incomplete item/sentence. Then if the list are in brackets "[]", add 
            bracket in the tail to make it a grammarly correct list. You should just output the final result.
                    
            Example:
            Input: The ids and names of the albums from Lana Del Rey are [{{'id': '5HOHne1wzItQlIYmLXLYfZ', 'name': "Did you know that there's a tunnel under Ocean Blvd"}}, {{'id': '2wwCc6fcyhp1tfY3J6Javr', 'name': 'Blue Banisters'}}, {{'id': '6Qeos  
            Output: The ids and names of the albums from Lana Del Rey are [{{'id': '5HOHne1wzItQlIYmLXLYfZ', 'name': "Did you know that there's a tunnel under Ocean Blvd"}}, {{'id': '2wwCc6fcyhp1tfY3J6Javr', 'name': 'Blue Banisters'}}]
                    
            Begin!
            Input: {truncated_str}
            Output:  
            """;

}