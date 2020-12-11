// All NetworKit ops that compute a Double edge attribute on a graph.
package main

import (
	"fmt"
	"log"
	"math"
	"runtime/debug"

	"github.com/lynxkite/lynxkite/sphynx/networkit"
)

func init() {
	operationRepository["NetworKitComputeDoubleEdgeAttribute"] = Operation{
		execute: func(ea *EntityAccessor) (err error) {
			defer func() {
				if e := recover(); e != nil {
					err = fmt.Errorf("%v", e)
					log.Printf("%v\n%v", e, string(debug.Stack()))
				}
			}()
			vs := ea.getVertexSet("vs")
			es := ea.getEdgeBundle("es")
			weight := ea.getDoubleAttributeOpt("weight")
			o := &NetworKitOptions{ea.GetMapParam("options")}
			seed := uint64(1)
			if s, exists := o.Options["seed"]; exists {
				seed = uint64(s.(float64))
			}
			networkit.SetSeed(seed, true)
			networkit.SetThreadsFromEnv()
			// The caller can set "directed" to false to create an undirected graph.
			g := ToNetworKit(vs, es, weight, o.Options["directed"] != false)
			defer networkit.DeleteGraph(g)
			g.IndexEdges()
			var result networkit.DoubleVector
			switch ea.GetStringParam("op") {
			case "ForestFireScore":
				c := networkit.NewForestFireScore(
					g, o.Double("spread_prob"), o.Double("burn_ratio"))
				defer networkit.DeleteForestFireScore(c)
				c.Run()
				result = c.Scores()
			}
			attr := &DoubleAttribute{
				Values:  make([]float64, len(es.Src)),
				Defined: make([]bool, len(es.Src)),
			}
			for i := range attr.Values {
				// The NetworKit edge IDs don't correspond to the Sphynx edge IDs.
				// TODO: Can we do better than mapping the results back by src/dst?
				id := g.EdgeId(uint64(es.Src[i]), uint64(es.Dst[i]))
				attr.Values[i] = result.Get(int(id))
				attr.Defined[i] = !math.IsNaN(attr.Values[i])
			}
			return ea.output("attr", attr)
		},
	}
}
