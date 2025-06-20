# Distributed Food Delivery System Emulator

This project emulates the core functionality of a distributed system, similar to applications like **efood** or **Wolt**, for food delivery services.

## How to Run

To run the system, follow these steps in order:

1. **Start the Master Server**  
   This component coordinates the entire system and should be started first:
   ```bash
   java MasterServer


2. **Start the First Worker Node**
   Add the first delivery worker node to the system by running:

   ```bash
   java WorkerNode w1 localhost 6001 localhost 5555
   ```

    * `w1` is the ID of the worker
    * `localhost 6001` is the address and port of the worker
    * `localhost 5555` is the address and port of the master server

3. **Start the Master Control Console**
   This simulates an administrator interface for managing the master server:

   ```bash
   java MasterConsole localhost 5555
   ```

4. **Start the Client Console**
   This simulates an end-user client interacting with the application:

   ```bash
   java ClientConsole localhost 5555
   ```
