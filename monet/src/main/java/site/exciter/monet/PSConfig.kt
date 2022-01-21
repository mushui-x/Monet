package site.exciter.monet

import java.io.Serializable

/**
 * @Description: PSConfig
 * @Author: ZhangJie
 * @CreateDate: 2021/12/28 4:51 下午
 */
class PSConfig(builder: Builder) : Serializable {
    /**
     * 是否需要裁剪
     */
    var needCrop: Boolean

    class Builder : Serializable {
        var needCrop = false

        fun needCrop(needCrop: Boolean): Builder {
            this.needCrop = needCrop
            return this
        }

        fun build(): PSConfig {
            return PSConfig(this)
        }
    }

    init {
        needCrop = builder.needCrop
    }
}