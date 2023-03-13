package com.sunya.netchdf.parser

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
                "/media/twobee/netch/hdf4/jeffmc/swath.hdf",
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

            Arguments.of(
                "/media/twobee/netch/hdf4/jeffmc/swath.hdf",
"""
GROUP=SwathStructure
    GROUP=SWATH_1
		SwathName="L2B Rainfall Products"
		GROUP=Dimension
			OBJECT=Dimension_1
				DimensionName="npix"
				Size=392
			END_OBJECT=Dimension_1
			OBJECT=Dimension_2
				DimensionName="nscan"
				Size=2002
			END_OBJECT=Dimension_2
		END_GROUP=Dimension
		GROUP=DimensionMap
		END_GROUP=DimensionMap
		GROUP=IndexDimensionMap
		END_GROUP=IndexDimensionMap
		GROUP=GeoField
			OBJECT=GeoField_1
				GeoFieldName="Time"
				DataType=DFNT_FLOAT64
				DimList=("nscan")
			END_OBJECT=GeoField_1
			OBJECT=GeoField_2
				GeoFieldName="Latitude"
				DataType=DFNT_FLOAT32
				DimList=("nscan","npix")
			END_OBJECT=GeoField_2
			OBJECT=GeoField_3
				GeoFieldName="Longitude"
				DataType=DFNT_FLOAT32
				DimList=("nscan","npix")
			END_OBJECT=GeoField_3
		END_GROUP=GeoField
		GROUP=DataField
			OBJECT=DataField_1
				DataFieldName="Rain Status"
				DataType=DFNT_INT16
				DimList=("nscan","npix")
			END_OBJECT=DataField_1
			OBJECT=DataField_2
				DataFieldName="Rain Rate"
				DataType=DFNT_INT16
				DimList=("nscan","npix")
			END_OBJECT=DataField_2
			OBJECT=DataField_3
				DataFieldName="Rain Type"
				DataType=DFNT_INT8
				DimList=("nscan","npix")
			END_OBJECT=DataField_3
			OBJECT=DataField_4
				DataFieldName="Surface Type"
				DataType=DFNT_INT8
				DimList=("nscan","npix")
			END_OBJECT=DataField_4
		END_GROUP=DataField
		GROUP=MergedFields
		END_GROUP=MergedFields
	END_GROUP=SWATH_1
END_GROUP=SwathStructure
GROUP=GridStructure
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
    fun testOdlParser(filename: String, odl: String) {
        println("$odl")
        // val odlstruct = OdlParser.parseToEnd(odl)
        val odlstruct = ODLparser().parseFromString(odl)
        println("org = \n${odlstruct}")
        val odlt = ODLtransform().transform(odlstruct)
        println("massage = \n${odlt}")
    }

}