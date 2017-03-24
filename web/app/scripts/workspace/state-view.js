'use strict';

// Viewer of a state at an output of a box.

angular.module('biggraph')
 .directive('stateView', function(side, util) {
    return {
      restrict: 'E',
      templateUrl: 'scripts/workspace/state-view.html',
      scope: {
        stateId: '='
      },
      link: function(scope) {
        scope.$watch('stateId', function() {
          if (scope.stateId) {
            scope.state = util.nocache(
              '/ajax/getOutput',
              { id: scope.stateId }
            );
          }
        });
        scope.$watch('state.$resolved', function() {
          if (scope.state && scope.state.$resolved &&
              scope.state.kind === 'project') {
            scope.side = new side.Side([], '');
            scope.side.project = scope.state.project;
            scope.side.project.$resolved = true;
            scope.side.onProjectLoaded();
          } else {
            scope.side = undefined;
          }
        });
      }
    };
});
