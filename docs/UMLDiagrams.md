# UML Diagrams Index

**Project:** Lumiroom: AI-Assisted Mobile AR Furniture Visualization and Interior Planning System  
**Version:** 1.0  
**Date:** 2026-06-10  

[⬅ Back to README](../README.md) | [Next: C4 Architecture](C4Architecture.md)

---

## Overview
Lumiroom leverages UML 2.x standards for rigorous system modeling. Due to the scale of the system, diagrams are logically separated into specific markdown files to prevent diagram bloat.

### Structural Diagrams
- **[Class Diagrams](ClassDiagrams.md)**: Details the Domain Use Cases, ViewModels, and Repository structures.
- **[ER Diagrams](ERDiagrams.md)**: Details the exact SQLite Room database schema relationships.

### Behavioral Diagrams
- **[Use Case Diagrams](UseCases.md)**: Actor relationships and system boundaries.
- **[Activity Diagrams](ActivityDiagrams.md)**: Branching logic flows, such as parsing Voice Commands.
- **[State Machine Diagrams](StateMachineDiagrams.md)**: The lifecycle of AR Entities (Selected, Hidden, Locked, Moving).

### Interaction Diagrams
- **[Sequence Diagrams](SequenceDiagrams.md)**: Time-ordered interactions for complex actions like Cloud Syncing.
- **[Data Flow Diagrams](DataFlowDiagrams.md)**: Unidirectional Data Flow mappings from Room -> ViewModel -> Compose UI.

### Architectural Diagrams
- **[C4 Architecture Models](C4Architecture.md)**: Simon Brown's C4 abstraction levels.
- **[Deployment Diagrams](DeploymentDiagrams.md)**: Hardware/Software deployment nodes.
