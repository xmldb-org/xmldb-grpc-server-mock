/*
 * Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
 * writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.xmldb.grpc.server;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Flow;

import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.grpc.ResourceData;

import com.google.protobuf.ByteString;

class ResourceDataSubscription implements Flow.Subscription {
  private final ResourceData.Builder builder;
  private final StreamConsumer streamConsumer;
  private final Flow.Subscriber<? super ResourceData> subscriber;
  private final byte[] buffer;

  private boolean canceled = false;

  ResourceDataSubscription(ResourceData.Builder builder, int chunkSize,
      StreamConsumer streamConsumer, Flow.Subscriber<? super ResourceData> subscriber) {
    this.builder = builder;
    this.streamConsumer = streamConsumer;
    this.subscriber = subscriber;
    this.buffer = new byte[chunkSize];
    subscriber.onSubscribe(this);
  }

  @Override
  public void request(long maxData) {
    try (PipedOutputStream outputStream = new PipedOutputStream();
        PipedInputStream inputStream = new PipedInputStream(outputStream)) {
      streamConsumer.accept(outputStream);
      int read = 0;
      for (long totalRead = 0; totalRead < maxData && !canceled; totalRead += read) {
        if (inputStream.available() == 0) {
          subscriber.onComplete();
          return;
        }
        read = inputStream.read(buffer);
        if (read == -1) {
          subscriber.onComplete();
          return;
        } else {
          subscriber.onNext(builder.setDataChunk(ByteString.copyFrom(buffer, 0, read)).build());
        }
      }
    } catch (IOException | XMLDBException e) {
      subscriber.onError(e);
    }
  }

  @Override
  public void cancel() {
    canceled = true;
  }
}
