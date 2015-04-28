(import com.codeshelf.integration.PickSimulator)
(import  com.codeshelf.flyweight.command.NetGuid)

(def pickSim (PickSimulator. lw/*manager*, (NetGuid. "0x11111111")))
(.loginAndSetup pickSim "BART")
(.setupOrderIdAsContainer pickSim "40433396" "1")
(.scanCommand pickSim "START")
(.logCheDisplay pickSim)

(.scanSomething pickSim "6061046")
(.scanSomething pickSim "SCANSKIP")

(.buttonPress pickSim 1 1)
(.setup pickSim)
(.scanCommand pickSim "CLEAR")
