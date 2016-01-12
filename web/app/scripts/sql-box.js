// Presents the parameters for running SQL scripts.
'use strict';

angular.module('biggraph').directive('sqlBox', function(side, util) {
  return {
    restrict: 'E',
    scope: { side: '=' },
    templateUrl: 'sql-box.html',
    link: function(scope) {
      scope.runQuery = function() {
        scope.result = util.nocache(
          '/ajax/runQuery',
          {
            project: scope.side.state.projectName,
            sql: scope.sql,
          });
      };

      scope.reportSQLError = function() {
        util.reportRequestError(scope.result, 'Error executing query.');
      };
    }
  };
});
