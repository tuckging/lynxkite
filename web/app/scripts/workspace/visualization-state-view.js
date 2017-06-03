'use strict';

// Viewer of a plot state.

angular.module('biggraph')
  .directive('visualizationStateView', function(util, side) {
    return {
      restrict: 'E',
      templateUrl: 'scripts/workspace/visualization-state-view.html',
      scope: {
        stateId: '=',
        popupModel: '=',
      },
      link: function(scope) {

        scope.$watch('stateId', function(newValue, oldValue, scope) {
          scope.sides = [];
          scope.left = new side.Side(scope.sides, 'left', scope.stateId);
          scope.right = new side.Side(scope.sides, 'right', scope.stateId);
          scope.sides.push(scope.left);
          scope.sides.push(scope.right);

          scope.visualization = util.get('/ajax/getVisualizationOutput', {
            id: scope.stateId
          });
          scope.visualization.then(
            function() {
              scope.applyVisualizationData();
            }, function() {}
          );
        });

        scope.applyVisualizationData = function() {
          if (scope.visualization.$resolved) {
            var state = {
              left: undefined,
              right: undefined,
            };
            if (scope.visualization.state) {
              state = JSON.parse(scope.visualization.state);
            }
            scope.left.updateFromBackendJson(state.left);
            scope.right.updateFromBackendJson(state.right);
            scope.left.reload();
            scope.right.reload();
          }
        };

        function getLeftToRightBundle() {
          var left = scope.left;
          var right = scope.right;
          if (!left.loaded() || !right.loaded()) { return undefined; }
          // If it is a segmentation, use "belongsTo" as the connecting path.
          if (right.isSegmentationOf(left)) {
            return left.getBelongsTo(right);
          }
          // If it is the same project on both sides, use its internal edges.
          if (left.project.name === right.project.name) {
            return left.project.edgeBundle;
          }
          return undefined;
        }

        function getRightToLeftBundle() {
          var left = scope.left;
          var right = scope.right;
          if (!left.loaded() || !right.loaded()) { return undefined; }
          // If it is the same project on both sides, use its internal edges.
          if (left.project.name === right.project.name) {
            return left.project.edgeBundle;
          }
          return undefined;
        }

        scope.$watchGroup(
          ['left.project.$resolved', 'right.project.$resolved'],
          function(result) {
            var leftLoaded = result[0];
            var rightLoaded = result[1];
            if (leftLoaded) {
              scope.left.onProjectLoaded();
            }
            if (rightLoaded) {
              scope.right.onProjectLoaded();
            }
            if (leftLoaded || rightLoaded) {
              scope.leftToRightBundle = getLeftToRightBundle();
              scope.rightToLeftBundle = getRightToLeftBundle();
              // scope.applyVisualizationData();
            }

          });

      },
    };
  });
