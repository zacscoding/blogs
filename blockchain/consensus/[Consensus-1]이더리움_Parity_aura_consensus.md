## POA : Parity - Aura(Authority Round)  

- [Define of step](#define_of_step)
- [Primary Node](#primary_node)
- [Block Difficulty](#block_difficulty)
- [Example](#example)
- [Finality](#finality)
- [Reference](#reference)

<div id="define_of_step"></div>  

#### Define step  

step=UNIX_TIME/t, 여기서 t는 duration

<div id="primary_node"></div>  

#### Primary Node  

index=step mod n, 여기서 n은 signer 수  

<div id="block_difficulty"></div>  

#### Block Difficulty  

max_value(==340282366920938463463374607431768211455) + parent_step - current_step + current_empty_steps  

```
// in parity source
// Chain scoring: total weight is sqrt(U256::max_value())*height - step
fn calculate_score(parent_step: U256, current_step: U256, current_empty_steps: U256) -> U256 {
	U256::from(U128::max_value()) + parent_step - current_step + current_empty_steps
}
```

<div id="example"></div>   

#### Example  

signers = ["node0", "node1", "node2"] 이고 step duration=5 일 때 아래와 같이 계산

- block number 1
  - timestamp = 1528343110
  - step = 305668622 (== 1528343110 / 5)
  - primary node index = 2 (== 305668622 / 3)
  - => miner == node2

- block number 2
  - timestamp = 1528343145
  - step = 305668629 (== 1528343145 / 5)
  - primary node index = 0 (== 305668629 / 3)
  - => miner == node0  

```
## Latest block number : 9
## Num : 0, Step : 0, Timestamp : 0 ===> null || dif : 0 || Primary : 0
## Num : 1, Step : 305668622, Timestamp : 1528343110 ===> node2 || timestamp / step : 5 || Primary : 2
## Num : 2, Step : 305668629, Timestamp : 1528343145 ===> node0 || timestamp / step : 5 || Primary : 0
## Num : 3, Step : 305668630, Timestamp : 1528343150 ===> node1 || timestamp / step : 5 || Primary : 1
## Num : 4, Step : 305668649, Timestamp : 1528343245 ===> node2 || timestamp / step : 5 || Primary : 2
## Num : 5, Step : 305668653, Timestamp : 1528343265 ===> node0 || timestamp / step : 5 || Primary : 0
## Num : 6, Step : 305668654, Timestamp : 1528343270 ===> node1 || timestamp / step : 5 || Primary : 1
## Num : 7, Step : 305668676, Timestamp : 1528343380 ===> node2 || timestamp / step : 5 || Primary : 2
## Num : 8, Step : 305668680, Timestamp : 1528343400 ===> node0 || timestamp / step : 5 || Primary : 0
## Num : 9, Step : 305668681, Timestamp : 1528343405 ===> node1 || timestamp / step : 5 || Primary : 1
```  

=> chain spec에 validators의 인덱스별로 node0, node1, node2이 validators.
=> 위의 result에서 step은 Parity json result이고, Primary 값은 step % 3(노드 수)  
=> 즉 step 은 timestamp / step duration(config 값) && primary는 step % n(노드 수)로 결정  

<div id="finality"></div>  

#### Finality  
; TODO



<div id="reference"></div>   

#### Reference  

- parity aura  
https://wiki.parity.io/Pluggable-Consensus#validator-engines
- parity multi set  
https://wiki.parity.io/Validator-Set.html  

---  
