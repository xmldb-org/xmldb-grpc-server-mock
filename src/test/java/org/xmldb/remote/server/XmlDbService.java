/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.xmldb.remote.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmldb.api.grpc.EmptyRequest;
import org.xmldb.api.grpc.SystemInfo;
import org.xmldb.api.grpc.XmlDbServiceGrpc;

import io.grpc.stub.StreamObserver;

public class XmlDbService extends XmlDbServiceGrpc.XmlDbServiceImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlDbService.class);

  @Override
  public void systemInfo(EmptyRequest request, StreamObserver<SystemInfo> responseObserver) {
    LOGGER.info("getSystemInfo({})", request);
    final var systemInfoBuilder = SystemInfo.newBuilder();
    systemInfoBuilder.getJavaVersionBuilder() //
        .setDate(System.getProperty("java.version.date")) //
        .setVersion(System.getProperty("java.version")) //
        .setVendor(System.getProperty("java.vendor")) //
        .setVendorVersion(System.getProperty("java.vendor.version"));
    systemInfoBuilder.setSystemVersion("1.0.0");
    final var systemInfo = systemInfoBuilder.build();
    responseObserver.onNext(systemInfo);
    responseObserver.onCompleted();
    LOGGER.info("getSystemInfo({}) done", systemInfo);
  }
}
