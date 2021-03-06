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

var frisby = require('frisby');

frisby.create('Login to StreamSets Data Collector')
  .get(browser.baseUrl + 'login?j_username=admin&j_password=admin')
  .expectStatus(200)
  .expectHeader('Content-Type', 'text/html')
  .after(function(body, res) {
    var cookie = res.headers['set-cookie'];


    /**
     * GET rest/v1/admin/logout
     */
    frisby.create('Should be able to logout SDC')
      .post(browser.baseUrl + 'rest/v1/logout', {
        headers:  {
          "Cookie": cookie
        }
      })
      .expectStatus(200)
      .after(function(body, res) {
        var cookie = res.headers['set-cookie'];
        frisby.create('Trying to access build info after logout should redirect to login page.')
          .get(browser.baseUrl + 'rest/v1/system/threads', {
            headers:  {
              "Content-Type": "application/json",
              "Accept": "application/json",
              "Cookie": cookie
            }
          })
          .expectStatus(200)
          .expectHeaderContains('content-type', 'text/html')
          .toss();
      })
      .toss();
  })
  .toss();
