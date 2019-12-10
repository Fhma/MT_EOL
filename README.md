# Mutation Testing on EOL Programs
This repository contains an experiment of mutation testing against EOL candidate programs to evaluate EOL mutation operators implemented using Epsilon Mutator (EMU).

## Requirements
1. Epsilon (version: 1.3): it can be found [here](https://eclipse.org/epsilon/download/).
3. Eclipse Modelling Framework (version 2.12): it can be obtained from Eclipse update site
4. EMFCompare (version 3.x): it can be obtained from Eclipse update site

## Experiment resources
The experiment resources are located in the folder (EOL_resources), which contains the following:

1. candidates: contains candidate EOL programs.
2. inModels_generator: contains EMG code used to semi-automatically generate input models. The model-generator project can be found [here](https://github.com/Fhma/Model-Generator).
3. inModels: contains input models used to executed EOL programs.
4. expectedModels: contains expected after executing EOL programs. Models were generated using the Java project
(uk.ac.york.cs.emu.eol.examples.executor).
5. metamodels: contains metamodels expressed in Ecore to load/write input/output models.
6. operators: contains mutation operators expressed in EMU.
7. mutations: contains mutations generated from executing all operators on EOL programs. Mutations were generated using Java project (uk.ac.york.cs.emu.eol.examples.mutations.generator), and then executed using the Java project (uk.ac.york.cs.emu.eol.examples.mutations.executor).
