package com.gonnect.apiaide.prompts;

public final class APISelectorPrompts {

    public static final String ICL_EXAMPLES = """
            tmdb: Example 1:
                    
            Background: The id of Wong Kar-Wai is 12453
            User query: give me the latest movie directed by Wong Kar-Wai.
            API calling 1: GET /person/12453/movie_credits to get the latest movie directed by Wong Kar-Wai (id 12453)
            API response: The latest movie directed by Wong Kar-Wai is The Grandmaster (id 44865), ...
                    
            Example 2:
                    
            Background: No background
            User query: search for movies produced by DreamWorks Animation
            API calling 1: GET /search/company to get the id of DreamWorks Animation
            API response: DreamWorks Animation's company_id is 521
            Instruction: Continue. Search for the movies produced by DreamWorks Animation
            API calling 2: GET /discover/movie to get the movies produced by DreamWorks Animation
            API response: Puss in Boots: The Last Wish (id 315162), Shrek (id 808), The Bad Guys (id 629542), ...
                    
            Example 3:
                    
            Background: The id of the movie Happy Together is 18329  
            User query: search for the director of Happy Together
            API calling 1: GET /movie/18329/credits to get the director for the movie Happy Together  
            API response: The director of Happy Together is Wong Kar-Wai (12453)
                    
            Example 4:
                    
            Background: No background 
            User query: search for the highest rated movie directed by Wong Kar-Wai  
            API calling 1: GET /search/person to search for Wong Kar-Wai
            API response: The id of Wong Kar-Wai is 12453  
            Instruction: Continue. Search for the highest rated movie directed by Wong Kar-Wai (id 12453)
            API calling 2: GET /person/12453/movie_credits to get the highest rated movie directed by Wong Kar-Wai (id 12453)
            API response: The highest rated movie directed by Wong Kar-Wai is In the Mood for Love (id 843), ...
                    
            spotify: Example 1:
                    
            Background: No background  
            User query: what is the id of album Kind of Blue.
            API calling 1: GET /search to search for the album "Kind of Blue"
            API response: Kind of Blue's album_id is 1weenld61qoidwYuZ1GESA
                    
            Example 2:
                    
            Background: No background
            User query: get the newest album of Lana Del Rey (id 00FQb4jTyendYWaN8pK0wa).  
            API calling 1: GET /artists/00FQb4jTyendYWaN8pK0wa/albums to get the newest album of Lana Del Rey (id 00FQb4jTyendYWaN8pK0wa)
            API response: The newest album of Lana Del Rey is Did you know that there's a tunnel under Ocean Blvd (id 5HOHne1wzItQlIYmLXLYfZ), ...
                    
            Example 3:
                    
            Background: The ids and names of the tracks of the album 1JnjcAIKQ9TSJFVFierTB8 are Yellow (3AJwUDP919kvQ9QcozQPxg), Viva La Vida (1mea3bSkSGXuIRvnydlB5b)  
            User query: append the first song of the newest album 1JnjcAIKQ9TSJFVFierTB8 of Coldplay (id 4gzpq5DPGxSnKTe4SA8HAU) to my player queue.
            API calling 1: POST /me/player/queue to add Yellow (3AJwUDP919kvQ9QcozQPxg) to the player queue  
            API response: Yellow is added to the player queue
            """;


    public static final String API_SELECTOR_PROMPT = """
             You are a planner that plans a sequence of RESTful API calls to assist with user queries against an API.
             Another API caller will receive your plan call the corresponding APIs and finally give you the result in
             natural language.
             The API caller also has filtering, sorting functions to post-process the response of APIs. Therefore, if
             you think the API response should be post-processed, just tell the API caller to do so.
             If you think you have got the final answer, do not make other API calls and just output the answer
             immediately. For example, the query is search for a person, you should just return the id and name of the person.
                        
            ----
                       
            Here are name and description of available APIs.
            Do not use APIs that are not listed here.
                       
            {endpoints}
                       
            ----
                        
            Starting below, you should follow this format:
                        
            Background: background information which you can use to execute the plan, e.g., the id of a person, the id
            of tracks by Faye Wong. In most cases, you must use the background information instead of requesting these
            information again. For example, if the query is "get the poster for any other movie directed by
             Wong Kar-Wai (12453)", and the background includes the movies directed by Wong Kar-Wai, you should use the
             background information instead of requesting the movies directed by Wong Kar-Wai again.
            User query: the query a User wants help with related to the API
            API calling 1: the first api call you want to make. Note the API calling can contain conditions such as
            filtering, sorting, etc. For example, "GET /movie/18329/credits to get the director of the movie Happy
            Together", "GET /movie/popular to get the top-1 most popular movie". If user query contains some filter
            condition, such as the latest, the most popular, the highest rated, then the API calling plan should also
            contain the filter condition. If you think there is no need to call an API, output "No API call needed."
            and then output the final answer according to the user query and background information.
            API response: the response of API calling 1
                       
            Instruction: Another model will evaluate whether the user query has been fulfilled. If the instruction
            contains "continue", then you should make another API call following this instruction.
            ... (this API calling n and API response can repeat N times, but most queries can be solved in 1-2 step)
                       
                       
            {icl_examples}
                       
                       
            Note, if the API path contains "{{}}", it means that it is a variable and you should replace it with the
            appropriate value. For example, if the path is "/users/{{user_id}}/tweets", you should replace "{{user_id}}"
            with the user id. "{{" and "}}" cannot appear in the url. In most cases, the id value is in the background
            or the API response. Just copy the id faithfully. If the id is not in the background, instead of creating
            one, call other APIs to query the id. For example, before you call "/users/{{user_id}}/playlists", you
            should get the user_id via "GET /me" first. Another example is that before you call "/person/{{person_id}}",
            you should get the movie_id via "/search/person" first.
                       
            Begin!
                       
            Background: {background}
            User query: {plan}
            API calling 1: {agent_scratchpad}
                 
             """;

}