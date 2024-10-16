package com.stabledata

import com.stabledata.synchro.DataRequest
import com.stabledata.synchro.DataResponse
import com.stabledata.synchro.SynchroGrpcServiceGrpc
import io.grpc.stub.StreamObserver

class GrpcService : SynchroGrpcServiceGrpc.SynchroGrpcServiceImplBase() {
    override fun getData(request: DataRequest, responseObserver: StreamObserver<DataResponse>) {
        val response = DataResponse.newBuilder()
            .setData("Data for id: ${request.id}")
            .build()

        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}