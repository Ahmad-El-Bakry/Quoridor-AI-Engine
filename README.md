# Quoridor AI Strategy Engine

A full-stack, decoupled web application implementing the classic abstract board game **Quoridor**. This project features a robust simulation engine built with Java and Spring Boot, a responsive vanilla frontend UI, and tiered AI components ranging from simple pathfinding heuristics to advanced Monte Carlo Tree Search (MCTS).

---

## 🎥 Project Demo Video
Click the link below to watch the technical demonstration and gameplay walkthrough:
👉 you want it to be a clean, clickable hyperlink, you can change it to exactly this:
[Watch the Demo Video Here](https://drive.google.com/drive/folders/1YHfS8u3kF346GiKhus-h5vomJpYp9ZTU?usp=sharing)

---

## 🎮 Game Description
Quoridor is played on a 9x9 grid. The objective is to be the first player to advance your pawn to any square on the opponent's starting baseline row. 

### Core Rules:
1. **On your turn:** You may either move your pawn one square orthogonally (forward, backward, left, or right) OR place a single wall segment.
2. **Wall Placement:** Walls are placed in the gaps between squares. They block movement across that boundary for both players.
3. **The Absolute Rule:** You are legally forbidden from placing a wall that completely traps a player. At least one valid path to the goal row must remain open for both pawns at all times.
4. **Pawn Jumps:** When two pawns face each other orthogonally with no wall between them, a player may jump directly over the opponent's pawn.

---

## 🖼️ Game Screenshots
Below are visual demonstrations of the system interface and runtime environment:

### Main Gameplay Screen
![Main Game Interface](https://via.placeholder.com/800x450.png?text=Main+Gameplay+Interface+Window)
*(Tip: Replace this link with a screenshot of your working web browser app!)*

### Deep AI Computation Mode
![AI Thinking Overlay](https://via.placeholder.com/800x450.png?text=AI+Thinking+Spinner+Overlay)

---

## 🛠️ Installation and Running Instructions

Because this system utilizes a **decoupled full-stack architecture**, the backend server and the frontend client must be run side-by-side.

### Prerequisites:
* **Java Development Kit (JDK 17 or higher)**
* A modern web browser (Chrome, Edge, Firefox)

### 1. Starting the Backend Server
1. Open your terminal inside the root project directory (the `backend` folder containing `pom.xml`).
2. Run the following command to compile the project and start the Spring Boot server:
```bash
   ./mvnw spring-boot:run