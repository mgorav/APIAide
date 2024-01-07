# APIAide: Connecting Large Language Models with REST APIs

## Introduction

APIAide enables large language models (LLMs) to understand OpenAPI specifications of REST APIs and make API calls to accomplish complex user instructions.

Interacting with production APIs has challenges like:

- Consuming extensive documentation across parameters, schemas
- Planning optimal sequences of API calls
- Handling authentication, marshaling arguments, parsing tricky responses

APIAide augments LLMs to overcome these via four capabilities:

* **REST API Comprehension:** Ingest OpenAPI specifications for LLMs to understand API semantics
* **Task Decomposition:** Break down instructions into coherent API call sequences using learned policies
* **API Parameterization:** Handle auth, arguments to correctly invoke APIs
* **Response Parsing:** Reliably extract information from API outputs using response schemas

This handles real-world API complexity. Next, we cover the techniques powering APIAide.


## Mathematical Foundations

APIAide harnesses mathematical models to power its ability to interact with complex real-world APIs reliably. Specifically, Markov Decision Processes enable optimized planning, vector embeddings facilitate accurate API matching, and policy learning parsers handle tricky JSON responses.

These techniques allow APIAide to break down instructions, determine optimal sequences of API calls, configure parameters correctly, and extract information from outputs. The formal abstractions provide provable capabilities to extrapolate intelligently to new situations based on prior experiences.

The mathematical foundations supply APIAide the rigor required to operate at industrial grade. The composable structure also allows improving the specialized modules independently.

Enabled by mathematics, APIAide can take on intricate multi-step interactions with production APIs that otherwise remain out of reach. Let us explore its technical core.

### Markov Decision Processes for Effective Decomposition

Markov Decision Process (MDP) provides a mathematical framework for modeling decision making in situations where outcomes are partly random and partly under the control of a decision maker. It enables optimizing policies to maximize reward.

An MDP is defined by:

- **States (s)**: Represent the current state of the environment
- **Actions (a)**: Choices available to the decision maker in each state
- **Transition Function (P(s'|s,a))**: Probability of transitioning to next state s' by taking action a in current state s
- **Reward Function (R(s,a))**: Specifies scalar reward r for state s after action a
- **Discount Factor (γ)**: Controls priority for current vs future rewards

The aim is to learn a policy π(a|s) that decides the action a in state s, by maximizing cumulative discounted reward:

```
π* = argmaxπ ∑ P(s′|s, a) [R(s,a) + γV(s')]
```

Where V(s) is the value function that recursively accumulates rewards from current state onward per policy π.

In APIAide, the MDP formulation is:

- **States:** Encode partial progress on user's instruction
- **Actions:** Generate next natural language sub-task
- **Transition Function:** Depends on environment's API call outcomes
- **Reward:** +1 for successfully completing instruction, 0 otherwise

This allows the **Planner** module to learn an optimal policy to break down instructions into coherent sequences of sub-tasks for efficient API usage.

The policy maximizes the long term cumulative reward of fulfilling user instructions through dynamic programming over API call outcomes.

> NOTE Generic Explanation
>
> Markov decision processes (MDPs) are a mathematical framework for modeling decision making under uncertainty. They are useful for problems that involve sequential decision making, where the current state and action affect future states.
> The key components of an MDP are:
> - States (S): The set of possible states the process can be in. For example, in a robot navigation problem, the states could be the robot's x,y location.
> - Actions (A): The set of possible actions that can be taken in each state. For example, the robot's actions could be to move left, right, up or down.
> - Transition probabilities (P): The probability of transitioning from state s to s' under action a. Often represented as P(s'|s,a).
> - Rewards (R): The reward received after transitioning from state s to s' under action a. Represented as R(s,a,s'). The agent aims to maximize cumulative reward.
> - Discount factor (γ): A factor between 0 and 1 that determines the importance of future rewards. 0 makes the agent focus only on immediate rewards, while values approaching 1 make it strive for long-term high rewards.
> The agent's goal is to find the optimal policy π* that maximizes expected cumulative discounted rewards:
> π* = argmax π ∑t=0∞ γt R(st,at,st+1)
> Where the actions at are selected according to policy π. MDPs can be solved exactly using dynamic programming algorithms like value iteration and policy iteration. They can also be approximated using reinforcement learning algorithms like Q-learning.

**_Example_**
States:

S = {s1, s2, s3, s4}

s1: User profile data incomplete  
s2: User profile data complete
s3: Product recommendations incomplete
s4: Product recommendations complete

Actions:

A = {a1, a2, a3, a4}

1. a1: Call user profile API
2. a2: Call user activity API  
3. a3: Call product recommendation API
4. a4: Filter products based on user profile

Transition Probabilities:

P(s2|s1,a1) = 0.9 // Get profile data
P(s2|s1,a2) = 0.7 // Get activity data

P(s3|s2,a3) = 0.8 // Get recommended products
P(s4|s3,a4) = 0.9 // Filter products

Rewards:

R(s2) = 10 // Reward for complete user profile
R(s4) = 40 // Reward for tailored recommendations

γ = 0.8 // Prioritize long-term rewards

This models the process of orchestrating user profile APIs, recommendation APIs, and product filtering to provide personalized ecommerce recommendations. The agent learns to maximize the long-term reward through optimal API sequences.


### Leveraging Embeddings for Optimal API Matching

Embeddings are dense vector representations that encode semantics of text sequences. APIAide's API Selector module uses embeddings to match user sub-tasks to optimal API sequences.

**Embeddings Generation**

Embeddings are generated for:

- **Sub-tasks (p)**: Encodes user's natural language sub-task intent
- **API Sequences (a)**: Encodes API call chains based on OpenAPI spec

These embeddings are extracted from the large language model using mean-pooling operation over token embeddings.

```
p, a ∈ Rd
```

Where,
- **d**: Embedding dimension size (typical values of 512, 768, 1024)

**Similarity Computation**

Cosine similarity between sub-task embedding ***p*** and API sequence embedding ***a*** is then calculated as:

```
similarity = cos(θ) = (p.a) / (||p||.||a||)
```

Where θ is the angle between the vectors p and a.

**API Selection**

The API sequence ã with maximum cosine similarity to the sub-task is selected:

```
ã = argmaxa cos(p, a)
```

This semantic matching retrieves the API call sequence best encoding the intent of the natural language sub-task.

**Key Benefits**

- Handles vocabulary mismatch between sub-tasks and APIs
- Understands conceptual similarities
- Robust to minor language variations

So vector similarities between embedded spaces act as the substrate for APIAide's logical planning.

**_Example_**

Here is an example of using embeddings for API sequence matching in an ecommerce recommendation system:

**Sub-tasks:**

p1: "Get user purchase history"
p2: "Retrieve product catalog"
p3: "Filter products by category"

**API Sequences:**

a1: "User API -> Purchases API"
a2: "Catalog API -> Products API"
a3: "Products API -> Category Filter API"

**Embeddings Generation:**

Sub-task and API sequence embeddings are generated using sentence-transformers.

p1, p2, p3, a1, a2, a3 ∈ R512

**Similarity Computation:**

cos(p1, a1) = 0.89
cos(p2, a2) = 0.94
cos(p3, a3) = 0.82

**API Selection:**

Select API sequences with maximum cosine similarity:

p1 mapped to a1
p2 mapped to a2
p3 mapped to a3

This allows matching natural language sub-tasks to optimal API sequences using semantic similarity of learned embeddings, enabling contextual recommendation orchestration.

### Parser Policy Learning for Optimal Code Generation

The parser learns a parameterized policy for generating Python code that extracts responses accurately.

**Policy Definition**

The policy π′θ is learned using parameters θ. It maps the response schema S and query q to parsing code c:

```
c = π'θ(S, q)
```

**Reinforcement Learning Formulation**

Policy gradient reinforcement learning is employed to optimize this policy by maximizing a reward.

The key components are:

- **State:** Response schema S, query q
- **Action:** Generate parsing code tokens
- **Reward:** Execution accuracy on unseen responses

**Policy Gradient Objective**

Mathematically, the parser maximizes the expected cumulative reward J using gradient ascent on policy parameters θ:

```
∇θ J(π′θ) = E [∇θ log π'θ(at|st) Rt]
```

Where,

- t indexes token generation timesteps when creating parsing code
- st is state, at is action, rt is reward at timestep t
- Rt is cumulative future reward from t

Intuitively, parameter updates nudge the policy towards token sequences that result in accurate Python code for parsing responses.

**Benefits**

By formulizing as a reinforcement learning problem, the parser can handle variability in JSON responses and improve over time through accrued experiences.

The learned policy enables optimally extracting information from tricky API responses.

**_Example_**

Here is an example of using policy learning for response parsing in an ecommerce product API:

**Policy Definition**

πθ maps product response schema and extraction query to parsing code

**State**

Product response schema:

```
{
  "id": int,
  "name": str, 
  "price": float,
  "categories": [str] 
}
```

Extraction query: "Get product categories"

**Actions**

Generate parsing code tokens:

categories = response["categories"]

**Rewards**

+1 if extraction works on new responses, -1 if fails

**Policy Optimization**

Use REINFORCE to maximize extraction accuracy:

```
∇θ J(πθ) = E[ ∇θ log πθ(a|s) * reward ] 
```

**Benefits**

- Handles variability in nested JSON responses
- Improves over time as API usage accrues

This allows reliably extracting relevant fields from ecommerce API outputs.

### Applying Formal Models for Integrating OpenAPIs

The mathematical models enable standardized integration by providing common semantic frameworks for information exchange between components.

For example, the Markov Decision Process powering the planner provides provable ways to translate natural language instructions into plan state representations. This allows binding external state transition systems like ChatGPT API.

Similarly, the vector embeddings facilitate matching text to API sequences across vocabulary gaps. This technique allows discovering alignments between APIAide's internal actions and operation specifications exposed by external APIs.

Concretely, APIAide utilizes OpenAPIs from models like ChatGPT providing OpenAPI specifications for capabilities like text completions.

The interaction workflow becomes:

- **User Instruction**: Processed by APIAide Core
- **Mapped to OAS**: Via ontologies to ChatGPT's API Schema
- **ChatGPT Invocation**: Executed via generated bindings
- **Response Parsing**: Results consolidated back

Additionally, APIAide's modular architecture allows injecting other OpenAPIs, like image tagging systems exposing vector embedding based APIs.

By leaning on community standards like OpenAPI coupled with mathematical models acting as connective tissue, APIAide facilitates systemic integration rather than domain-specific optimization.

**_Example_**

Continuing the ecommerce example:

**Integrating ChatGPT API**

1. User instruction: "Explain this product to potential buyers"

2. APIAide core decomposes instruction

3. ChatGPT API spec exposes text generation endpoint

4. Bind to OpenAPI operation:

```
prompt = product_description 
completion = chatgpt.generate(prompt)
```

5. Invoke API with product details

6. Parse and return generated text

**Integrating Image Tagging API**

1. User: "Categorize these product images"

2. APIAide prepares image embeddings

3. Vision API has clustering endpoint accepting vectors

4. Discover alignment and bind API:

```
clusters = vision.cluster_images(image_embeddings)
```

5. Pass image vectors and parse clusters

By leveraging mathematical models, APIAide can integrate APIs like ChatGPT and vision APIs to accomplish ecommerce tasks.

## System Architecture

APIAide features specialized modules and hierarchical planning with feedback:


### Modular Components

```mermaid
graph TD
    U[(Instructions)]--> P{Planner}
    P --> S{API Selector}
    S --> C{Caller}
    C --> R{REST API}
    R --> Pr{Response Parser}
    Pr --> P
```

Tasks are divided into planning, API selection, calling and parsing for separation of concerns.

### API Plan Execution

The executor module handles executing the API sequence:

```mermaid
graph TD
    APISeq-->CC{API <br> Caller}
    CC-->RR{REST <br> API}
    RR-->PP{Response <br> Parser}
    PP-->CC
```

The caller CC handles authentication, parameterization to invoke the REST API RR.

The parser PP then processes the API response to extract relevant information.

> The API Caller module uses annotation driven CGLIB proxying to automatically instrument API calls with appropriate prompts and headers required for each endpoint. This avoids manual configuration and customization for each API client.

### Multi-Level Planning Dynamically Adapts

Instead of creating all required API sequences upfront, APIAide employs a hierarchical planning approach with multiple levels of abstraction.

The key idea is to first create a high-level sub-task, and then map it to a detailed API call sequence. This provides flexibility to dynamically adjust the planning based on execution outcomes.

Workflow Steps

The iterative workflow is:

Decompose Query: Break down instruction into a high-level natural language sub-task
Match APIs: Select API sequence to accomplish the sub-task
Invoke APIs: Call APIs by preparing arguments, authorization etc.
Parse Response: Extract information from API output
Revise Sub-Task: Use extracted info to create next sub-task
Benefits

Handles unforeseen scenarios during execution
Avoids recreating entire plan from scratch
Adjusts to feedback through hierarchical structure
For example, if authentication fails, the framework can just replan the steps rather than planning from scratch.

This hierarchical planning centered around high-level sub-tasks followed by detailed API sequences enables APIAide to robustly handle complexity of production APIs.

```mermaid 
sequenceDiagram
    User->>Instruction Decomposer: User Instruction
    Instruction Decomposer->>API Matcher: High-Level <br> Sub-Task
    API Matcher->>API Invoker: Specific <br> API Sequence
    API Invoker->>REST API: Invoke API 
    REST API-->>Response Parser: API <br> Response
    Response Parser-->>Instruction Decomposer: Extracted <br> Information
    Instruction Decomposer->>Instruction Decomposer: Generate Next <br> Sub-Task
```

The **Instruction Decomposer** module first breaks down the complex user instruction into a high-level natural language sub-task.

The **API Matcher** then understands the sub-task intent and selects the appropriate sequence of REST API calls to accomplish it.

Then the **API Invoker** handles authentication, parameterization and actually invokes the APIs, getting the raw response.

The **Response Parser** processes this response based on API schemas to extract relevant information.

Finally, the **Instruction Decomposer** uses this extracted info to create the next sub-task, and the loop continues.

This two-level hierarchy of creating high-level sub-tasks first and then matching them to detailed API call sequences provides flexibility. Based on execution outcomes, the sub-tasks can be revised dynamically.

For example, if authentication fails while invoking an API, the framework can replan the steps without needing to recreate everything from scratch.

This adaptive nature enables APIAide to handle unforeseen scenarios when working with production REST APIs.


## Scalable and Flexible Implementation

APIAide is designed to be technology-agnostic and can be implemented in various stacks. A robust reference architecture is shown below:

```mermaid
graph LR
    subgraph Backend
        J((Java))
        Jobs>Job Queue]
        Cache>Caching Layer]
    end
    
    subgraph Language Models
        L4J([Lang4J])
        L4J --- Inference>Inference Cluster]
    end

    J --- Plan[Planning <br> Module]
    J --- Select[API Selection <br> Module]

    Plan --->  L4J
    Select ---> L4J

    L4J ---> Call[API Invocation <br> Module]
    L4J ---> Parse[Response <br> Parsing Module]

    Parse ---> Cache
    Call ---> Cache
    Plan ---> Cache
    Select ---> Cache
```

**Java and Supporting Platforms** provide the scale, concurrency and tooling required for robustness. The key components it handles are:

- Asynchronous **Job Queue** for high throughput
- Distributed **Caching** for low latency
- Logging, Monitoring and Alerting capabilities

**Lang4J** serves as the interface to LLMs providing the intelligence required for planning, API selection etc.

For scale, multiple **Inference Instances** can be launched to handle heavy loads.

The workflow is:

1. Inputs first checked in cache
2. Cache misses queued as async jobs
3. Jobs processed by Lang4J + LLMs
4. Outputs cached

**Benefits**

- Leverages maturity of Java ecosystem
- Horizontally scalable for load handling
- Simplifies infra and deployment

This blueprint combines stability and intelligence to offer a robust API interaction platform.


## Technology Stack Powering APIAide

APIAide employs a robust tech stack to offer stability, scalability and ease of use.

| Layer | Technology                    | Responsibilities |
|-|-------------------------------|-|  
| Platform | Java, Spring Boot             | Scale, concurrency, monitoring |   
| Delivery | Kubernetes, Docker            | CI/CD, high availability |
| Intelligence | LangChain, LLMs (LangChain4j) | Planning, parsing capabilities |
| Data | MongoDB, Elasticsearch        | Storage, analysis ready data |
| Infrastructure | AWS, GCP                      | Reliability, security, compliance |
| Instrumentation | CGLIB                         | Automated API client prompting |
**Platform Layer**

Java and Spring Boot provide a high performance backend with dependency injection, web servicing and operational tooling baked in.

**Delivery Layer**

Containerized deployment on Kubernetes clusters provides portability across cloud providers while handling scale and uptime through infra automation.

**Intelligence Layer**

Framework like LangChain offer a clean interface for large language models to drive planning and response parsing logic.

**Data Layer**

Managed document stores like MongoDB and search engines like Elasticsearch provide the data storage and analytical capabilities.

**Infrastructure Layer**

By leveraging cloud infrastructure, the solution can meet security, compliance and reliability requirements out-of-the-box without own hosting.

Together these layers deliver an enterprise-grade platform for deploying APIAide capabilities for production usages with minimal risks.

## Assessing Generalization Across Complexity Frontiers

APIAide is evaluated across diverse dimensions spanning varying levels of complexity using real-world Spotify and TMDB APIs.

**Production-Grade APIs**

- TMDB: Movie database with 54 endpoints
- Spotify: Music platform with 40 APIs

**Testing Corpus**

100+ human annotated instructions covering:

- Query complexity levels
- API combinations required
- Parameter configurations

**Evaluation Dimensions**

- **Accuracy** across query complexity spectrum
- **Planning Efficiency** via gap between predicted and actual API sequences
- **Scalability** by increasing number of callable APIs

**Analysis Framework**

By tracing performance gradients instead of aggregate scores, granular insights emerge on strengths and areas needing improvement.

For example, decrement in success rate for longer instruction chains reveals challenges to scale to complex workflows.

**Results Summary**

| Metric | Outcome |
|-|-|  
| Correct API Call Chains | 79% |
| End-to-End Success Rate | 75% |

**Use Case**

*Create playlist "Love Coldplay" with most popular songs by artist*

**Key Takeaways**

✅ Assesses across complexity frontiers spanning simplicity to intricate multi-hop scenarios

✅ Quantifies scaling behavior on critical dimensions

✅ Provides confidence in handling real-world production complexity

The rigorous methodology evaluates the spectrum of capabilities required in deployment - understanding, planning, configuring and extracting information from APIs.

## Future Work

While results are encouraging, challenges remain:

- Performance drop on longer sequence lengths
- Brittleness to unseen APIs
- Blackbox failure modes of LLMs

Continued research across model design, testing and transparency is required to make systems like APIAide robust enough for production use.

## High-Impact Application Areas

While the initial focus has been on ad-hoc instructions, APIAide has promising potential to drive impact across multiple domains:

**Retail & Ecommerce**

- Catalog management workflows
- Personalization driven by CRM data
- Order fulfillment orchestration

**Healthcare**

- Patient record management
- Insurance claim processing
- Drug discovery pipelines

**Finance**

- Investment research automation
- Risk analysis decisioning
- Regulatory compliance

**Transportation & Logistics**

- Fleet route optimization
- Warehouse task coordination
- Delivery workflow orchestration

The capabilities to understand APIs, decompose tasks, invoke services and consolidate responses can unlock new efficiencies.


## High-Impact Application Areas

While the initial focus has been on ad-hoc instructions, APIAide has promising potential to drive impact across multiple domains:

**Retail & Ecommerce**

- Catalog management workflows
- Personalization driven by CRM data
- Order fulfillment orchestration

**Healthcare**

- Patient record management
- Insurance claim processing
- Drug discovery pipelines

**Finance**

- Investment research automation
- Risk analysis decisioning
- Regulatory compliance

**Transportation & Logistics**

- Fleet route optimization
- Warehouse task coordination
- Delivery workflow orchestration

**Personalization and Recommendations**

- Build dynamic customer segments based on CRM data
- Make real-time product suggestions across channels
- Orchestrate data from catalog, inventory, browsing history etc.

**Customer Journey Optimization**

- Choose optimal channels and content for each segment
- Coordinate messaging across email, web, app push
- React to emerging trends and sales events

**Advertising and Promotion**

- Create and rapidly iterate ad campaigns
- Integrate performance data from multiple analytics sources
- Adjust bids and budgets to optimize ROI

**Sentiment and Market Analysis**

- Scrape public social data to detect trends
- Combine reviews, ratings, mentions into single dashboard
- Feed insights into product, pricing decisions


The capabilities to understand APIs, decompose tasks, invoke services and consolidate responses can unlock new efficiencies.

As APIAide matures, it can transition from serving individuals to powering large-scale automated orchestration between organizational systems.

### Vast Potential with Democratization

Furthermore, by hiding complexity behind a conversational interface, APIAide promises to democratize API usage for less technical users.

Subject matter experts in areas like medicine, logistics etc. can focus on core problems while APIAide handles the drudgery of integration.

This shift towards empowering more domain experts also expands the possibility frontier for AI systems to positively impact domains.

Thus the addressable use cases for large language model based orchestrators scale manifold as the interfaces become accessible to non-programmers. The future is promising!