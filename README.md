# RISC160016CPU32B1Vx
Instruction Set Architecture based on 160016 with RISC approach

## Contents

<details>
<summary>Assembler</summary>
* Assembly examples
* Save binary executables in ELF-format*
* Macro-assembler (A little more practical for programmer)
* Micro-instruction between-layer output for debugging (1:1 representation of assembly instruction to binary)
* Bindump of assembled binary output for debugging
* Disassembler
</details>

<details>
<summary>Interpreter</summary>
* Emulate instruction-per-instruction runtime
* Read binary executables in ELF-format
* Interrupt handler
</details>

<details>
<summary>User Interface for program runtime</summary>
* Runtime inputs
  * Keyboard input
  * Mouse input*
  * Console input
  * Timer input
* Runtime outputs
  * Console output
  * Screen output
  * Character screen output
  * Datastream output*
* Runtime observers
  * Memory observer (up to 256kb per observer, read/write indicator)
  * Registerfile observer
  * Runtime statistics
</details>
