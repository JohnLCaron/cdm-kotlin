package com.sunya.netchdf.parser

import com.sunya.netchdf.hdf4.ODLparseFromString
import com.sunya.netchdf.hdf4.ODLtransform
import com.sunya.netchdf.hdf4.ODLparser
import java.util.*
import java.util.stream.Stream

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class OdlParseTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
/*            Arguments.of(
                testData + "netchdf/hdf4/jeffmc/swath.hdf",
                """
GROUP=SwathStructure
    GROUP=SWATH_1
	END_GROUP=SWATH_1
END_GROUP=SwathStructure
GROUP=GridStructure
END_GROUP=GridStructure
GROUP=PointStructure
END_GROUP=PointStructure
END
                """.trimMargin()
            ),

 */

            Arguments.of("whatever",
"""
GROUP=SwathStructure
END_GROUP=SwathStructure
GROUP=GridStructure
	GROUP=GRID_1
		GridName="GeometricParameters"
		XDim=8
		YDim=32
		UpperLeftPointMtrs=(7460750.000000,1090650.000000)
		LowerRightMtrs=(7601550.000000,527450.000000)
		Projection=GCTP_SOM
		ProjParams=(6378137,-0.006694,0,98018013.750000,67030010.880000,0,0,0,98.880000,0,0,180,0)
		SphereCode=12
		PixelRegistration=HDFE_CENTER
		GROUP=Dimension
			OBJECT=Dimension_1
				DimensionName="SOMBlockDim"
				Size=180
			END_OBJECT=Dimension_1
		END_GROUP=Dimension
		GROUP=DataField
			OBJECT=DataField_1
				DataFieldName="SolarAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_1
			OBJECT=DataField_2
				DataFieldName="SolarZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_2
			OBJECT=DataField_3
				DataFieldName="DfAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_3
			OBJECT=DataField_4
				DataFieldName="DfZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_4
			OBJECT=DataField_5
				DataFieldName="CfAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_5
			OBJECT=DataField_6
				DataFieldName="CfZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_6
			OBJECT=DataField_7
				DataFieldName="BfAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_7
			OBJECT=DataField_8
				DataFieldName="BfZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_8
			OBJECT=DataField_9
				DataFieldName="AfAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_9
			OBJECT=DataField_10
				DataFieldName="AfZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_10
			OBJECT=DataField_11
				DataFieldName="AnAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_11
			OBJECT=DataField_12
				DataFieldName="AnZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_12
			OBJECT=DataField_13
				DataFieldName="AaAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_13
			OBJECT=DataField_14
				DataFieldName="AaZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_14
			OBJECT=DataField_15
				DataFieldName="BaAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_15
			OBJECT=DataField_16
				DataFieldName="BaZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_16
			OBJECT=DataField_17
				DataFieldName="CaAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_17
			OBJECT=DataField_18
				DataFieldName="CaZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_18
			OBJECT=DataField_19
				DataFieldName="DaAzimuth"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_19
			OBJECT=DataField_20
				DataFieldName="DaZenith"
				DataType=DFNT_FLOAT64
				DimList=("SOMBlockDim","XDim","YDim")
			END_OBJECT=DataField_20
		END_GROUP=DataField
		GROUP=MergedFields
		END_GROUP=MergedFields
	END_GROUP=GRID_1
END_GROUP=GridStructure
GROUP=PointStructure
END_GROUP=PointStructure
END
"""
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("params")
    fun testOdlParser(filename: String, structMetatdata: String) {
        println("$structMetatdata")
        // val odlstruct = OdlParser.parseToEnd(odl)
        val odlstruct = ODLparseFromString(structMetatdata)
        println("org = \n${odlstruct}")
        val odlt = ODLtransform(odlstruct)
        println("transformed = \n${odlt}")
    }

}