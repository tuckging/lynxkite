// Parameters

def getParameter(paramName, defaultValue) {
    if (params.containsKey(paramName))
       return params[paramName]
    else
       return defaultValue
}

vertices = getParameter('vertices', '30')
ebSize=getParameter('ebSize', '5')
splits=getParameter('splits', '10')
furtherUndefinedAttr1=getParameter('fa1','5')
furtherUndefinedAttr2=getParameter('fa2','5')
seed= getParameter('seed', '31415')
mostCallsPossible=getParameter('mostCalls', '3');
splitProb= getParameter('splitProb', '0.3')

seed2=(seed.toInteger() + 42).toString()

furtherUndefinedAttr1Expr =
        '(originalUniqueId >= ' +
        splits +
        ' && ' +
        splits +
        ' + ' +
        furtherUndefinedAttr1 +
        ' > originalUniqueId) ? 1.0 : 0.0'


furtherUndefinedAttr2Expr =
        '(originalUniqueId >= ' +
        splits +
        ' + ' +
        furtherUndefinedAttr1 +
        ' && ' +
        splits +
        ' + ' +
        furtherUndefinedAttr1 +
        ' + ' +
        furtherUndefinedAttr2 +
        ' > originalUniqueId) ? 1.0 : 0.0'



//
//
// The Mash() function used in this program
// comes from here:
// https://github.com/nquinlan/better-random-numbers-for-javascript-mirror
// It is under MIT license.
jsprogram=
"""
// From http://baagoe.com/en/RandomMusings/javascript/
// Johannes Baagøe <baagoe@baagoe.com>, 2010
function Mash() {
  var n = 0xefc8249d;
 

  var mash = function(data) {
    data = data.toString();
    for (var i = 0; i < data.length; i++) {
      n += data.charCodeAt(i);
      var h = 0.02519603282416938 * n;
      n = h >>> 0;
      h -= n;
      h *= n;
      n = h >>> 0;
      h -= n;
      n += h * 0x100000000; // 2^32
    }
    return (n >>> 0) * 2.3283064365386963e-10; // 2^-32
  };

  mash.version = 'Mash 0.9';
  return mash;
}

function Rnd(seed1, seed2) {
  var mash = Mash();
  var seed = mash(seed1.toString() + '_' + seed2.toString());
  var incr = seed;
  var that = this;
  var unifRand = function() {
    var a = Math.sin(seed++) * 10000;
    var b = a - Math.floor(a);
    return b;
  }
  return {
    random: unifRand,
    geomChoose: function(id, p, lastId) {      
      for (var i = 0; i <= lastId; i++) {
          var q = unifRand();
          if (q < p) {
             if (i == id) return 1;
            else return 0;
          }
      }
      if (lastId == id) return 1;
      else return 0;
    },
  }
};



var srcSeed = src\$originalUniqueId
var dstSeed =  dst\$originalUniqueId
var srcCount = src\$split;
var dstCount = dst\$split;
var srcIdx = src\$index;
var dstIdx = dst\$index;
var edgeCnt = originalCalls
var prob = $splitProb

var total = srcCount * dstCount;
var myId = dstCount * srcIdx + dstIdx;
var lastId = total - 1;

function compute()
{
  if (total == 1) {
    return edgeCnt;
  }

  var randomFunc = Rnd(srcSeed, dstSeed).geomChoose
  
  var count = 0;

  for (var j = 0; j < edgeCnt; j++) {
    count += randomFunc(myId, prob, lastId);
  }

  return count;
}
compute();
"""


split=lynx.newProject('split test for FP')
split.newVertexSet(size: vertices)
split.createRandomEdgeBundle(degree: ebSize, seed: seed)
split.discardLoopEdges()
split.mergeParallelEdges()

split.addGaussianVertexAttribute(name: 'random', seed: seed)
split.addRankAttribute(keyattr: 'random', order: 'ascending', rankattr: 'originalUniqueId')

split.derivedVertexAttribute(
  output: 'split',
  expr: '(originalUniqueId < ' + splits + ') ? 2.0 : 1.0',
  type: 'double'
)

split.derivedVertexAttribute(
  output: 'furtherUndefinedAttr1',
  type: 'double',
  expr: furtherUndefinedAttr1Expr
)

split.derivedVertexAttribute(
  output: 'furtherUndefinedAttr2',
  type: 'double',
  expr: furtherUndefinedAttr2Expr
)

split.vertexAttributeToString(
  attr: 'originalUniqueId'
)


split.addRandomEdgeAttribute(
  name: 'originalCallsUnif',
  dist: 'Standard Uniform',
  seed: seed2
)

split.derivedEdgeAttribute(
  output: 'originalCalls',
  type: 'double',
  expr: 'Math.floor(originalCallsUnif * ' + mostCallsPossible + ');'
)

split.splitVertices(
  rep: 'split',
  idattr: 'newId',
  idx: 'index'
)

split.derivedVertexAttribute(
  output: 'attr1',
  expr: '(furtherUndefinedAttr1 == 1.0 || (split == 2.0 && index == 0)) ? undefined : originalUniqueId',
  type: 'string'
)

split.derivedVertexAttribute(
  output: 'attr2',
  expr: '(furtherUndefinedAttr2 == 1.0 || (split == 2.0 && index == 1)) ? undefined : originalUniqueId',
  type: 'string'
)

split.derivedEdgeAttribute(
  output: 'splitCalls',
  type: double,
  expr: jsprogram
)

split.filterByAttributes('filterea-splitCalls': '> 0.0')


// Do fingerprinting
split.fingerprintingBasedOnAttributes(
  leftName: 'attr1',
  rightName: 'attr2',
  weights: 'splitCalls',
  mo: '2',
  extra: '"weightingMode": "InDegree", "multiNeighborsPreference": 5.0, "alpha": -1.0',
  ms: '0.0'
)


split.fillWithConstantDefaultValue(
  attr: 'attr1',
  def: '-1'
)

split.fillWithConstantDefaultValue(
  attr: 'attr2',
  def: '-1'
)


split.derivedVertexAttribute(
  output: 'label',
  type: 'string',
  expr: 'originalUniqueId + "," + attr1 + "," + attr2'
)


split.derivedVertexAttribute(
  output: 'normal',
  type: 'double',
  expr: '(split == 1.0 && furtherUndefinedAttr1 == 0.0 && furtherUndefinedAttr2 == 0.0) ? 1.0 : 0.0'
)

split.derivedVertexAttribute(
  output: 'furtherOk',
  type: 'double',
  expr: '((furtherUndefinedAttr1 == 1.0 && attr1 == -1) || (furtherUndefinedAttr2 == 1.0 && attr2 == -1)) ? 1.0 : 0.0'
)

split.derivedVertexAttribute(
  output: 'furtherBad',
  type: 'double',
  expr: '((furtherUndefinedAttr1 == 1.0 && attr1 != -1) || (furtherUndefinedAttr2 == 1.0 && attr2 != -1)) ? 1.0 : 0.0'
)

split.derivedVertexAttribute(
  output: 'churnerFound',
  type: 'double',
  expr: '(split == 2.0 && attr1 == attr2) ? 1.0 : 0.0'
)

split.derivedVertexAttribute(
  output: 'churnerNoMatch',
  type: 'double',
  expr: '(split == 2.0 && (attr1 == -1 || attr2 == -1)) ? 1.0 : 0.0'
)

split.derivedVertexAttribute(
  output: 'churnerMisMatch',
  type: 'double',
  expr: '(split == 2.0 && attr1 != -1 && attr2 != -1 && attr2 != attr1) ? 1.0 : 0.0'
)

split.derivedVertexAttribute(
  output: 'labelType',
  type: 'string',
  expr:

  """
  function fun() {
  var tmp = "";
  tmp += normal == 1.0 ? "normal" : "";
  tmp += furtherOk == 1.0 ? "furtherOk" : "";
  tmp += furtherBad == 1.0 ? "furtherBad" : "";
  tmp += churnerFound == 1.0 ? "churnerFound" : "";
  tmp += churnerNoMatch == 1.0 ? "churnerNoMatch" : "";
  tmp += churnerMisMatch == 1.0 ? "churnerMisMatch" : "";
  return tmp;
  }
  fun();
  """
)


split.aggregateVertexAttributeGlobally(
  prefix: "",
  "aggregate-normal": "sum"
)

split.aggregateVertexAttributeGlobally(
  prefix: "",
  "aggregate-furtherOk": "sum"
)

split.aggregateVertexAttributeGlobally(
  prefix: "",
  "aggregate-furtherBad": "sum"
)

split.aggregateVertexAttributeGlobally(
  prefix: "",
  "aggregate-churnerFound": "sum"
)

split.aggregateVertexAttributeGlobally(
  prefix: "",
  "aggregate-churnerNoMatch": "sum"
)

split.aggregateVertexAttributeGlobally(
  prefix: "",
  "aggregate-churnerMisMatch": "sum"
)

vertices=split.scalars['vertex_count']
edges=split.scalars['edge_count']
normal=split.scalars['normal_sum']
furtherOk=split.scalars['furtherOk_sum']
furtherBad=split.scalars['furtherBad_sum']
churnerFound =split.scalars['churnerFound_sum']
churnerNoMatch =split.scalars['churnerNoMatch_sum']
churnerMisMatch =split.scalars['churnerMisMatch_sum']

println "vertices: $vertices"
println "edges: $edges"

println "normal $normal"
println "furtherOk $furtherOk"
println "furtherBad $furtherBad"
println "churnerFound $churnerFound"
println "churnerNoMatch $churnerNoMatch"
println "churnerMisMatch $churnerMisMatch"


