/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Controller for Library Pane Delete Modal.
 */

angular
  .module('dataCollectorApp.home')
  .controller('ResetOffsetModalInstanceController', function ($scope, $modalInstance, pipelineInfo, api) {
    angular.extend($scope, {
      showLoading: false,
      isOffsetResetSucceed: false,
      common: {
        errors: []
      },
      pipelineInfo: pipelineInfo,

      /**
       * Callback function Yes button
       */
      yes: function() {
        $scope.showLoading = true;
        api.pipelineAgent.resetOffset(pipelineInfo.name).
          success(function() {
            $scope.showLoading = false;
            $scope.isOffsetResetSucceed = true;
          }).
          error(function(data) {
            $scope.showLoading = false;
            $scope.common.errors = [data];
          });
      },

      /**
       * Callback function for No button
       */
      no: function() {
        $modalInstance.dismiss('cancel');
      },

      /**
       * Callback function for Close button
       */
      close: function() {
        $modalInstance.close(pipelineInfo);
      }
    });
  });