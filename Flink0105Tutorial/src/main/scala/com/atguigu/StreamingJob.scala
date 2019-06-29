package com.atguigu

// 隐式类型转换
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.time.Time

import org.apache.flink.cep.scala.CEP
import org.apache.flink.cep.scala.pattern.Pattern
import scala.collection.Map
case class LoginEvent(userId: String, ip: String, eventType: String, eventTime: String)
object StreamingJob {
  def main(args: Array[String]) {
    // set up the streaming execution environment
    // 获取运行时环境

    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)
    env.setParallelism(1)

    val loginEventStream = env.fromCollection(List(
      LoginEvent("1", "192.168.0.1", "fail", "1558430842"),
      LoginEvent("1", "192.168.0.2", "fail", "1558430843"),
      LoginEvent("1", "192.168.0.3", "fail", "1558430844"),
      LoginEvent("2", "192.168.10.10", "success", "1558430845")
    )).assignAscendingTimestamps(_.eventTime.toLong * 1000)

    val loginFailPattern = Pattern.begin[LoginEvent]("begin")
      .where(_.eventType.equals("fail"))
      .next("next")
      .where(_.eventType.equals("fail"))
//      .next("third")
//      .where(_.eventType.equals("fail"))
      .within(Time.seconds(10))

    val patternStream = CEP.pattern(loginEventStream.keyBy(_.userId), loginFailPattern)

    val loginFailDataStream = patternStream
        .select((pattern: Map[String, Iterable[LoginEvent]]) => {
          val first = pattern.getOrElse("begin", null).iterator.next()
          val second = pattern.getOrElse("next", null).iterator.next()

          (second.userId, second.ip, second.eventType)
        })

    env.execute

  }

}

