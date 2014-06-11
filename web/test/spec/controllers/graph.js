'use strict';

describe('metagraph navigation', function () {
  beforeEach(module('biggraph'));

  var ctrl, scope, $httpBackend;

  beforeEach(inject(function ($injector, $controller, $rootScope) {
    scope = $rootScope.$new();
    // Mock $httpBackend.
    $httpBackend = $injector.get('$httpBackend');
    var request = {id: 'test'};
    var requestJson = encodeURI(JSON.stringify(request));
    $httpBackend.when('GET', '/ajax/graph?q=' + requestJson).respond({
      'title': 'test node',
      'sources': [],
      'derivatives': [],
      'ops': [
        {
          'operationId': 0,
          'name': 'Find Maximal Cliques',
          'parameters': [{'title': 'Minimum Clique Size', 'defaultValue': '3'}],
        }
      ],
    });
    $httpBackend.when('GET', /ajax.startingOps/).respond([]);
    // Anything else gets an empty response.
    $httpBackend.when('GET', /ajax.*/).respond({});
    ctrl = $controller('GraphViewCtrl', {
      $scope: scope,
      $routeParams: {graph: 'test'},
    });
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('should load graph and stats data', function() {
    $httpBackend.flush();
    expect(scope.graph.ops.length).toBe(1);
  });
});
