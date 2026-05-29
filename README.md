# Quoridor AI Strategy Engine

A full-stack, decoupled web application implementing the classic abstract board game **Quoridor**. This project features a robust simulation engine built with Java and Spring Boot, a responsive vanilla frontend UI, and tiered AI components ranging from simple pathfinding heuristics to advanced Monte Carlo Tree Search (MCTS).

---

## 🎥 Project Demo Video
Click the link below to watch the technical demonstration and gameplay walkthrough:
👉 **[Watch the Demo Video Here](https://drive.google.com/drive/folders/1YHfS8u3kF346GiKhus-h5vomJpYp9ZTU?usp=sharing)** 👈

---

## 🎮 Game Description
Quoridor is played on a 9x9 grid. The objective is to be the first player to advance your pawn to any square on the opponent's starting baseline row. 

**Core Rules:**
1. **Movement:** On your turn, you may either move your pawn one square orthogonally (forward, backward, left, or right) OR place a single wall segment.
2. **Wall Placement:** Walls are placed in the gaps between squares and block movement across that boundary for both players.
3. **The Anti-Trapping Rule:** You are legally forbidden from placing a wall that completely traps a player. At least one valid path to the goal row must remain open at all times.
4. **Pawn Jumps:** When two pawns face each other orthogonally with no wall between them, a player may jump directly over the opponent's pawn.

---

## 🖼️ Screenshots of the Game in Action

### Main Gameplay Interface<img width="1455" height="702" alt="Screenshot 2026-05-29 035230" src="https://github.com/user-attachments/assets/a29fba04-cb42-4d04-844a-1b5a80ede504" />
<img width="1467" height="902" alt="Screenshot 2026-05-29 035126" src="https://github.com/user-attachments/assets/755add4a-bdd2-4b4b-bada-5b146a3d89f0" />
<img width="1369" height="912" alt="Screenshot 2026-05-29 035047" src="https://github.com/user-attachments/assets/bbcf9c13-7b06-457a-9927-afe2253259bb" />



### AI Calculation & Wall Placement
<img width="884" height="632" alt="Screenshot 2026-05-29 040702" src="https://github.com/user-attachments/assets/b07ff446-17cc-4c07-88d2-0754802b9108" />
<img width="1351" height="766" alt="Screenshot 2026-05-29 040646" src="https://github.com/user-attachments/assets/5f0c2e46-a21d-47c0-8c63-0d2694c30988" />
<img width="982" height="633" alt="Screenshot 2026-05-29 040425" src="https://github.com/user-attachments/assets/49992a3a-74ef-47ba-be66-55a9fadf007c" />
<img width="1324" height="760" alt="Screenshot 2026-05-29 040412" src="https://github.com/user-attachments/assets/60bc21e5-1f0d-4e9a-a9dc-28e058feee00" />


---

## 🛠️ Installation and Running Instructions

Because this system utilizes a decoupled full-stack architecture, you must run the backend server and the frontend client side-by-side.

### Prerequisites:
* **Java Development Kit (JDK 17 or higher)**
* A modern web browser (Chrome, Edge, Firefox)

### 1. Start the Backend Server (Java/Spring Boot)
1. Open a terminal inside the project's root directory.
2. Run the following command to compile the project and start the server:
   `.\mvnw spring-boot:run`
3. Wait until the terminal displays `Tomcat started on port 8080 (http)`. Leave this terminal running in the background.

### 2. Start the Frontend Game (HTML)
1. In your file explorer, locate the **`index.html`** file inside the project folder.
2. **Double-click `index.html`** to open it directly in your web browser.
3. The UI will instantly connect to the running Java backend, and the game is ready to play.

---

## 🕹️ Controls Explanation

The web application features an intuitive, entirely mouse-driven graphical user interface:

* **Pawn Movement:** Valid movement squares are automatically highlighted during your turn. Click any adjacent highlighted square to shift your pawn's coordinates.
* **Wall Placement:**
  1. Toggle your desired wall orientation using the letter **R** (**Horizontal** TO **Vertical** and vice versa).
  2. Hover your cursor over the alignment grids between the board cells.
  3. Click a gap to place your wall permanently. The server will reject the move if it violates the anti-trapping rule.
* **Timeline Controls:**
  * **Undo (⬅️):** Rolls back the last full round of moves using the system's underlying Event Sourcing history stack.
  * **Redo (➡️):** Re-applies an undone move sequence instantly.
  * **Reset:** Clears the board and starts a fresh game.
