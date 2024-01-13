package com.gonnect.apiaide.prompts;

import java.util.HashMap;
import java.util.Map;

public class PlannerPrompts {

    public static final Map<String, String> ICL_EXAMPLES = new HashMap<>();

    static {
        ICL_EXAMPLES.put("tmdb", "Example 1:\n" +
                "User query: give me some movies performed by Tony Leung.\n" +
                "Plan step 1: search person with name \"Tony Leung\"\n" +
                "API response: Tony Leung's person_id is 1337\n" +
                "Plan step 2: collect the list of movies performed by Tony Leung whose person_id is 1337\n" +
                "API response: Shang-Chi and the Legend of the Ten Rings, In the Mood for Love, Hero\n" +
                "Thought: I am finished executing a plan and have the information the user asked for or the data the used asked to create\n" +
                "Final Answer: Tony Leung has performed in Shang-Chi and the Legend of the Ten Rings, In the Mood for Love, Hero\n" +
                "\n" +
                "Example 2:\n" +
                "User query: Who wrote the screenplay for the most famous movie directed by Martin Scorsese?\n" +
                "Plan step 1: search for the most popular movie directed by Martin Scorsese\n" +
                "API response: Successfully called GET /search/person to search for the director \"Martin Scorsese\". The id of Martin Scorsese is 1032\n" +
                "Plan step 2: Continue. search for the most popular movie directed by Martin Scorsese (1032)\n" +
                "API response: Successfully called GET /person/{{person_id}}/movie_credits to get the most popular movie directed by Martin Scorsese. The most popular movie directed by Martin Scorsese is Shutter Island (11324)\n" +
                "Plan step 3: search for the screenwriter of Shutter Island\n" +
                "API response: The screenwriter of Shutter Island is Laeta Kalogridis (20294)\n" +
                "Thought: I am finished executing a plan and have the information the user asked for or the data the used asked to create\n" +
                "Final Answer: Laeta Kalogridis wrote the screenplay for the most famous movie directed by Martin Scorsese.");

        ICL_EXAMPLES.put("spotify", "Example 1:\n" +
                "User query: set the volume to 20 and skip to the next track.\n" +
                "Plan step 1: set the volume to 20\n" +
                "API response: Successfully called PUT /me/player/volume to set the volume to 20.\n" +
                "Plan step 2: skip to the next track\n" +
                "API response: Successfully called POST /me/player/next to skip to the next track.\n" +
                "Thought: I am finished executing a plan and completed the user's instructions\n" +
                "Final Answer: I have set the volume to 20 and skipped to the next track.\n" +
                "\n" +
                "Example 2:\n" +
                "User query: Make a new playlist called \"Love Coldplay\" containing the most popular songs by Coldplay\n" +
                "Plan step 1: search for the most popular songs by Coldplay\n" +
                "API response: Successfully called GET /search to search for the artist Coldplay. The id of Coldplay is 4gzpq5DPGxSnKTe4SA8HAU\n" +
                "Plan step 2: Continue. search for the most popular songs by Coldplay (4gzpq5DPGxSnKTe4SA8HAU)\n" +
                "API response: Successfully called GET /artists/4gzpq5DPGxSnKTe4SA8HAU/top-tracks to get the most popular songs by Coldplay. The most popular songs by Coldplay are Yellow (3AJwUDP919kvQ9QcozQPxg), Viva La Vida (1mea3bSkSGXuIRvnydlB5b).\n" +
                "Plan step 3: make a playlist called \"Love Coldplay\"\n" +
                "API response: Successfully called GET /me to get the user id. The user id is xxxxxxxxx.\n" +
                "Plan step 4: Continue. make a playlist called \"Love Coldplay\"\n" +
                "API response: Successfully called POST /users/xxxxxxxxx/playlists to make a playlist called \"Love Coldplay\". The playlist id is 7LjHVU3t3fcxj5aiPFEW4T.\n" +
                "Plan step 5: Add the most popular songs by Coldplay, Yellow (3AJwUDP919kvQ9QcozQPxg), Viva La Vida (1mea3bSkSGXuIRvnydlB5b), to playlist \"Love Coldplay\" (7LjHVU3t3fcxj5aiPFEW4T)\n" +
                "API response: Successfully called POST /playlists/7LjHVU3t3fcxj5aiPFEW4T/tracks to add Yellow (3AJwUDP919kvQ9QcozQPxg), Viva La Vida (1mea3bSkSGXuIRvnydlB5b) in playlist \"Love Coldplay\" (7LjHVU3t3fcxj5aiPFEW4T). The playlist id is 7LjHVU3t3fcxj5aiPFEW4T.\n" +
                "Thought: I am finished executing a plan and have the data the used asked to create\n" +
                "Final Answer: I have made a new playlist called \"Love Coldplay\" containing Yellow and Viva La Vida by Coldplay.");
    }

    public static final String PLANNER_PROMPT = """
            You are an agent that plans a solution to user queries.
            You should always give your plan in natural language.
            Another model will receive your plan and find the right API calls and give you the result in natural language.
            If you assess that the current plan has not been fulfilled, you can output "Continue" to let the API selector select another API to fulfill the plan.
            If you think you have got the final answer or the user query has been fulfilled, just output the answer immediately. If the query has not been fulfilled, you should continue to output your plan.
            In most cases, search, filter, and sort should be completed in a single step.
            The plan should be as specific as possible. It is better not to use pronouns in the plan but to use the corresponding results obtained previously.
            For example, instead of "Get the most popular movie directed by this person," you should output "Get the most popular movie directed by Martin Scorsese (1032)."
            If you want to iteratively query something about items in a list, then the list and the elements in the list should also appear in your plan.
            The plan should be straightforward. If you want to search, sort, or filter, you can put the condition in your plan.
            For example, if the query is "Who is the lead actor of In the Mood for Love (id 843)," instead of "get the list of actors of In the Mood for Love," you should output "get the lead actor of In the Mood for Love (843)."

            Starting below, you should follow this format:

            User query: {input}
            Plan step 1: {agent_scratchpad}
            {stop_signals}
            """;
}
