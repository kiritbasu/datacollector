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
 * Authorization Service
 */

angular.module('dataCollectorApp.common')
  .constant('userRoles', {
    admin: 'admin',
    creator: 'creator',
    manager: 'manager',
    guest: 'guest'
  })
  .service('authService', function($rootScope, $q, api) {
    var self = this;

    this.initializeDefer = undefined;

    /**
     * Initializes by fetching User Info
     *
     * @returns {*}
     */
    this.init = function() {
      if(!self.initializeDefer) {
        self.initializeDefer = $q.defer();

        $q.all([
          api.admin.getUserInfo()
        ])
          .then(function (results) {
            self.userInfo = results[0].data;
            self.initializeDefer.resolve();
          }, function(data) {
            self.initializeDefer.reject(data);
          });
      }

      return self.initializeDefer.promise;
    };

    /**
     * Checks if the logged in User Roles matches with the given list of roles.
     *
     * @param authorizedRoles
     * @returns {*}
     */
    this.isAuthorized = function (authorizedRoles) {
      if (!angular.isArray(authorizedRoles)) {
        authorizedRoles = [authorizedRoles];
      }

      var intersection = _.intersection(self.userInfo.roles, authorizedRoles);

      return intersection && intersection.length;
    };

    /**
     * Return Logged in User Name
     *
     * @returns {user|*|exports.config.params.login.user|Frisby.current.outgoing.auth.user|mapping.user|string}
     */
    this.getUserName = function() {
      return self.userInfo ? self.userInfo.user: '';
    };

    /**
     * Return User Roles
     * @returns {w.roles|*|string}
     */
    this.getUserRoles = function() {
      return self.userInfo ? self.userInfo.roles : [''];
    };
  });