<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <!-- needed for Swing's HTML renderer as it chokes on the meta tag. But does not work and needs externals postprocessing -->
  <xsl:output method="html" encoding="UTF-8" omit-xml-declaration="yes" indent="yes" media-type="text/html"
    doctype-public="" doctype-system="" />

  <xsl:param name="cssFile" />
  <xsl:param name="jsFile" />
  <!-- <xsl:param name="highlightedVars" /> -->


  <!-- start the main processing here building up the variables table -->
  <xsl:template match="/SEGMEM">
    <html>
      <head>
        <!-- <script src="{$jsFile}" type="text/javascript"></script> <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.5.2/jquery.min.js" 
          type="text/javascript"></script> <script language="javascript"> $(document).ready(function() { $("a").click(function() { $('table 
          tr td').slideToggle("medium"); }); }); </script> -->
        <link rel="stylesheet" type="text/css" href="{$cssFile}"></link>
      </head>
      <body>
<!--           <xsl:value-of select="$highlightedVars" /> -->
<!--         <button type="button" onclick="elementHideShow('ok')">Try it</button> <p id="ok" style="display:block">Clicked</p>  -->
        <table>
          <tr>
            <th title="Memory Region Variable">MemVar</th>
            <th title="Numeric Variable">NumVar</th>
            <th title="Bit-Range in Memory Region. Denotes if clobbered by a write (ro=read only till here, empty=written to)">Field</th>
            <th title="[Flag, Address]">PointsTo</th>
            <xsl:if test="//UNDEF">
                <th title="If a value is definitely undefined or depending on a flag variable">Undef</th>
            </xsl:if>
            <th title="Affine Equalities between variables">Affine</th>
            <xsl:if test="//CONGRUENCES">
                <th title="Multiplier and Offset of variable in Intervals">Congruence</th>
            </xsl:if>
            <xsl:if test="//INTERVALS">
              <th title="The value for the variable as one or more intervals">Intervals</th>
            </xsl:if>
            <xsl:if test="//THRESHOLDSWIDENING">
                <th title="Thresholds [Location, Threshold]">Widening</th>
            </xsl:if>
            <xsl:if test="//PREDICATESF">
                <th title="Bi-implications in the finite domain between flags and tests">Predicates(F)</th>
            </xsl:if>
            <xsl:if test="//PREDICATESZ">
                <th title="Implications in the zeno domain between tests">Predicates(Z)</th>
            </xsl:if>
          </tr>
          <xsl:apply-templates match="/" />
        </table>
        <br />
        <xsl:if test="//DELAYEDWIDENING">
            <xsl:call-template name="DelayedWideningTable" match="/" />
        </xsl:if>
      </body>
    </html>
  </xsl:template><!-- index Entries by their Values -->

  <xsl:key name="fields" match="FIELD/Entry" use="Variable" />
  <xsl:key name="congruence" match="CONGRUENCES/Entry" use="Variable" />
  <xsl:key name="supportset" match="SupportSet/Entry" use="Variable" />
  <xsl:key name="pointsto" match="POINTSTO/Entry" use="Variable | pointer/flag | pointer/address | pointer/flagsSum" />
  <xsl:key name="wideningthresholds" match="THRESHOLDSWIDENING/Entry" use="Value/Test/Equation/Term/Variable" />
  <xsl:key name="affine" match="AFFINE/Entry" use="Value/Term/Variable" />
  <xsl:key name="interval" match="INTERVALS/Entry" use="Variable" />
  <xsl:key name="undef" match="UNDEF/Entry" use="Variable" />
  <xsl:key name="predicatesz" match="PREDICATESZ/Entry" use="Premise/Equation/Term/Variable | Consequence/Equation/Term/Variable" />
  <xsl:key name="predicatesf" match="PREDICATESF/Entry" use="Premise/Equation/Term/Variable | Consequence/Equation/Term/Variable" />

  <xsl:template name="Fields">
    <xsl:apply-templates select="offset" mode="field_range_format" />
    <xsl:if test="Clobbered = 'false'">  (ro)</xsl:if>
  </xsl:template>

  <xsl:template name="Pointsto">
    <xsl:param name="Variable" />
    <xsl:variable name="pointstoTest" select="key('pointsto',$Variable)" />
    <xsl:choose>
      <xsl:when test="$pointstoTest">
        <xsl:apply-templates select="$pointstoTest/pointer" mode="pointer_format" />
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="Undef">
    <xsl:param name="Variable" />
    <xsl:variable name="undefTest" select="key('undef',$Variable)" />
    <xsl:choose>
      <xsl:when test="$undefTest">
        <xsl:if test="$undefTest[@type='Always']">
          <i>yes</i>
        </xsl:if>
        <xsl:if test="$undefTest/Flag">
          if <xsl:value-of select="$undefTest/Flag" />=0
        </xsl:if>
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="Affine">
    <xsl:param name="Variable" />
    <xsl:variable name="affineTest" select="key('affine',$Variable)" />
    <xsl:choose> <!--Affine -->
      <xsl:when test="$affineTest">
        <xsl:for-each select="$affineTest/Value">
          <xsl:apply-templates select="Term" mode="linear_format" />
          <xsl:choose>
            <xsl:when test="Constant > '0' ">+<xsl:value-of select="Constant" />=0</xsl:when>
            <xsl:when test="not(Constant > '-1' )"><xsl:value-of select="Constant" />=0</xsl:when> 
            <xsl:otherwise>=0</xsl:otherwise>
          </xsl:choose>
          <br />
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="Congruence">
    <xsl:param name="Variable" />
    <!-- find matching congruence entry -->
    <xsl:variable name="congruenceTest" select="key('congruence',$Variable)" />
    <xsl:choose>
      <xsl:when test="$congruenceTest">
        <xsl:apply-templates select="$congruenceTest/Value" mode="congruence_format" />
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="Interval">
    <xsl:param name="Variable" />
    <xsl:variable name="intervalTest" select="key('interval',$Variable)" />
    <xsl:choose>
      <xsl:when test="$intervalTest">
        <xsl:apply-templates select="$intervalTest/Value" mode="interval_format" />
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="ZenoTest">
    <xsl:param name="Test" />
    <xsl:apply-templates select="$Test/Equation/Term" mode="linear_format" />
    <xsl:variable name="constant" select="$Test/Equation/Constant" />
    <xsl:choose>
      <xsl:when test="not($Test/Equation/Term/text())"><xsl:value-of select="$constant" /></xsl:when>
      <xsl:when test="$constant > '0' ">+<xsl:value-of select="$constant" /></xsl:when>
      <xsl:when test="not($constant > '-1' )"><xsl:value-of select="$constant" /></xsl:when>
    </xsl:choose>
    <xsl:value-of select="$Test/Operator" />
  </xsl:template>

  <xsl:template name="FiniteTest">
    <xsl:param name="Test" />
    <xsl:apply-templates select="$Test/Equation[@side='left']/Term" mode="linear_format" />
    <xsl:variable name="leftConstant" select="$Test/Equation[@side='left']/Constant" />
    <xsl:choose>
      <xsl:when test="not($Test/Equation[@side='left']/Term/text())"><xsl:value-of select="$leftConstant" /></xsl:when>
      <xsl:when test="$leftConstant > '0' ">+<xsl:value-of select="$leftConstant" /></xsl:when>
      <xsl:when test="not($leftConstant > '-1' )"><xsl:value-of select="$leftConstant" /></xsl:when>
    </xsl:choose>
    <xsl:value-of select="$Test/Operator" />
    <xsl:apply-templates select="$Test/Equation[@side='right']/Term" mode="linear_format" />
    <xsl:variable name="rightConstant" select="$Test/Equation[@side='right']/Constant" />
    <xsl:choose>
      <xsl:when test="not($Test/Equation[@side='right']/Term/text())"><xsl:value-of select="$rightConstant" /></xsl:when>
      <xsl:when test="$rightConstant > '0' ">+<xsl:value-of select="$rightConstant" /></xsl:when>
      <xsl:when test="not($rightConstant > '-1' )"><xsl:value-of select="$rightConstant" /></xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="Predicatesz">
    <xsl:param name="Variable" />
    <xsl:variable name="predicateszTest" select="key('predicatesz',$Variable)" />
    <xsl:choose>
      <xsl:when test="$predicateszTest">
        <xsl:for-each select="$predicateszTest">
          <xsl:call-template name="ZenoTest">
            <xsl:with-param name="Test" select="Premise" />
          </xsl:call-template>
          &#8594;
          <xsl:call-template name="ZenoTest">
            <xsl:with-param name="Test" select="Consequence" />
          </xsl:call-template>
          <br />
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="Predicatesf">
    <xsl:param name="Variable" />
    <xsl:variable name="predicatesfTest" select="key('predicatesf',$Variable)" />
    <xsl:choose>
      <xsl:when test="$predicatesfTest">
        <xsl:for-each select="$predicatesfTest">
          <xsl:call-template name="FiniteTest">
            <xsl:with-param name="Test" select="Premise" />
          </xsl:call-template>
          &#8594;
          <xsl:call-template name="FiniteTest">
            <xsl:with-param name="Test" select="Consequence" />
          </xsl:call-template>
          <br />
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="WideningThresholds">
    <xsl:param name="Variable" />
    <xsl:variable name="wideningThresholdsTest" select="key('wideningthresholds',$Variable)" />
    <xsl:choose>
      <xsl:when test="$wideningThresholdsTest">
        <xsl:for-each select="$wideningThresholdsTest/Value">
          <xsl:variable name="originalTest" select="Origin/Test"/>
          <xsl:variable name="transformedTest" select="Test" />
          <xsl:choose>
<!--           both do not work, second one would work but needs XSLT 2.0 -->
            <xsl:when test="generate-id($originalTest) = generate-id($transformedTest)">
<!--             <xsl:when test="deep-equal( $originalTest, $transformedTest )"> -->
              <i>og/tr: </i>
              <xsl:call-template name="ZenoTest">
                <xsl:with-param name="Test" select="$originalTest" />
              </xsl:call-template>
              @<xsl:value-of select="Origin/Location" />
            </xsl:when>
            <xsl:otherwise>
              <i>og: </i>
              <xsl:call-template name="ZenoTest">
                <xsl:with-param name="Test" select="$originalTest" />
              </xsl:call-template>
              <i>@</i><xsl:value-of select="Origin/Location" />
              <br />
              <i>tr: </i>
              <xsl:call-template name="ZenoTest">
                <xsl:with-param name="Test" select="$transformedTest" />
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:if test="appliedAt/text()">
            <br />
            <i>used: </i><xsl:value-of select="appliedAt" />
          </xsl:if>
          <xsl:if test="not(position() = last())">
            <hr />
          </xsl:if>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="DelayedWideningTable">
    <xsl:template match="Entry[parent::DELAYEDWIDENING]">
      <table>
        <tr>
          <th title="Program Points with constant assignments.">Delayed Widening</th>
        </tr>
        <xsl:for-each select="DELAYEDWIDENING/Entry/ProgramPoint">
          <tr>
            <td>
              <xsl:value-of select="." />
            </td>
          </tr>
        </xsl:for-each>
      </table>
    </xsl:template>
  </xsl:template>

  <xsl:template match="Entry[parent::FIELD]">
    <!-- only memory regions which contain variables are interesting -->
    <xsl:if test="Variable/text()">
      <tr id="{generate-id()}">
        <td style="font-weight: bold">
          <xsl:value-of select="MemoryVariable" />
        </td><!-- Memory Variable -->
        <td style="font-weight: bold">
          <xsl:value-of select="Variable" />
        </td><!--Variable -->
        <td>
          <xsl:call-template name="Fields"></xsl:call-template>
        </td><!-- Fields -->
        <td><!--PointsTo -->
          <xsl:call-template name="Pointsto">
            <xsl:with-param name="Variable" select="Variable" />
          </xsl:call-template>
        </td>
        <xsl:if test="//UNDEF">
          <td><!-- Undef -->
            <xsl:call-template name="Undef">
              <xsl:with-param name="Variable" select="Variable" />
            </xsl:call-template>
          </td>
        </xsl:if>
        <td><!-- Affine -->
          <xsl:call-template name="Affine">
            <xsl:with-param name="Variable" select="Variable" />
          </xsl:call-template>
        </td>
        <xsl:if test="//CONGRUENCES">
          <td><!-- Congruence -->
            <xsl:call-template name="Congruence">
              <xsl:with-param name="Variable" select="Variable" />
            </xsl:call-template>
          </td>
        </xsl:if>
        <xsl:if test="//INTERVALS">
          <td><!--Intervals -->
            <xsl:call-template name="Interval">
              <xsl:with-param name="Variable" select="Variable" />
            </xsl:call-template>
          </td>
        </xsl:if>
        <xsl:if test="//THRESHOLDSWIDENING">
          <td> <!--WideningThresholds -->
            <xsl:call-template name="WideningThresholds">
              <xsl:with-param name="Variable" select="Variable" />
            </xsl:call-template>
          </td>
        </xsl:if>
        <xsl:if test="//PREDICATESF">
          <td> <!--Predicatesf -->
            <xsl:call-template name="Predicatesf">
              <xsl:with-param name="Variable" select="Variable" />
            </xsl:call-template>
          </td>
        </xsl:if>
        <xsl:if test="//PREDICATESZ">
          <td> <!--Predicatesz -->
            <xsl:call-template name="Predicatesz">
              <xsl:with-param name="Variable" select="Variable" />
            </xsl:call-template>
          </td>
        </xsl:if>
      </tr>
    </xsl:if>
  </xsl:template>

  <xsl:template match="Entry[parent::UNDEF]">
    <xsl:for-each select="Flag">
      <tr>
        <td></td><!-- Memory Variable -->
        <td style="font-weight: bold">
          <xsl:value-of select="." />
        </td><!--Variable -->
        <td></td><!-- Fields -->
        <td><!--PointsTo -->
          <xsl:call-template name="Pointsto">
            <xsl:with-param name="Variable" select="." />
          </xsl:call-template>
        </td>
        <xsl:if test="//UNDEF">
          <td><!-- Undef -->
            <xsl:call-template name="Undef">
              <xsl:with-param name="Variable" select="." />
            </xsl:call-template>
          </td>
        </xsl:if>
        <td><!--Affine -->
          <xsl:call-template name="Affine">
            <xsl:with-param name="Variable" select="." />
          </xsl:call-template>
        </td>
        <xsl:if test="//CONGRUENCES">
          <td><!-- Congruence -->
            <xsl:call-template name="Congruence">
              <xsl:with-param name="Variable" select="." />
            </xsl:call-template>
          </td>
        </xsl:if>
        <xsl:if test="//INTERVALS">
          <td><!--IntervalSet -->
            <xsl:call-template name="Interval">
              <xsl:with-param name="Variable" select="." />
            </xsl:call-template>
          </td>
        </xsl:if>
        <xsl:if test="//THRESHOLDSWIDENING">
          <td> <!--WideningThresholds -->
            <xsl:call-template name="WideningThresholds">
              <xsl:with-param name="Variable" select="." />
            </xsl:call-template>
          </td>
        </xsl:if>
        <xsl:if test="//PREDICATESF">
          <td> <!--Predicatesf -->
            <xsl:call-template name="Predicatesf">
              <xsl:with-param name="Variable" select="." />
            </xsl:call-template>
          </td>
        </xsl:if>
        <xsl:if test="//PREDICATESZ">
          <td> <!--Predicatesz -->
            <xsl:call-template name="Predicatesz">
              <xsl:with-param name="Variable" select="." />
            </xsl:call-template>
          </td>
        </xsl:if>
      </tr>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="Entry[parent::POINTSTO]">
    <!-- select the possible new variables introduced by the points-to domain but remove duplicates -->
    <xsl:for-each
      select="pointer/address[not(. = preceding::address)] | pointer/flag[not(. = preceding::flag)] | pointer/flagsSum[not(. = preceding::flagsSum)]">
      <xsl:if test="not(key('fields',.))">
        <tr>
          <td></td><!-- Memory Variable -->
          <td style="font-weight: bold">
            <xsl:value-of select="." />
          </td><!--Variable -->
          <td></td><!-- Fields -->
          <td></td><!--PointsTo information has been already handled. These are now new internal variables -->
          <xsl:if test="//UNDEF">
            <td><!-- Undef -->
              <xsl:call-template name="Undef">
                <xsl:with-param name="Variable" select="." />
              </xsl:call-template>
            </td>
          </xsl:if>
          <td><!--Affine -->
            <xsl:call-template name="Affine">
              <xsl:with-param name="Variable" select="." />
            </xsl:call-template>
          </td>
          <xsl:if test="//CONGRUENCES">
            <td><!-- Congruence -->
              <xsl:call-template name="Congruence">
                <xsl:with-param name="Variable" select="." />
              </xsl:call-template>
            </td>
          </xsl:if>
          <xsl:if test="//INTERVALS">
            <td><!--IntervalSet -->
              <xsl:call-template name="Interval">
                <xsl:with-param name="Variable" select="." />
              </xsl:call-template>
            </td>
          </xsl:if>
          <xsl:if test="//THRESHOLDSWIDENING">
            <td> <!--WideningThresholds -->
              <xsl:call-template name="WideningThresholds">
                <xsl:with-param name="Variable" select="." />
              </xsl:call-template>
            </td>
          </xsl:if>
          <xsl:if test="//PREDICATESF">
            <td> <!--Predicatesf -->
              <xsl:call-template name="Predicatesf">
                <xsl:with-param name="Variable" select="." />
              </xsl:call-template>
            </td>
          </xsl:if>
          <xsl:if test="//PREDICATESZ">
            <td> <!--Predicatesz -->
              <xsl:call-template name="Predicatesz">
                <xsl:with-param name="Variable" select="." />
              </xsl:call-template>
            </td>
          </xsl:if>
        </tr>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <!-- Ignore all the domains below as they do not introduce own variables -->
  <xsl:template match="SupportSet" />
  <xsl:template match="PREDICATESF" />
  <xsl:template match="PREDICATESZ" />
  <xsl:template match="DELAYEDWIDENING" />
  <xsl:template match="THRESHOLDSWIDENING" />
  <xsl:template match="AFFINE" />
  <xsl:template match="INTERVALS" />
  <xsl:template match="INTERVALSETS" />
  <xsl:template match="CONGRUENCES" /> 

  <!--  printers -->
  <xsl:template match="*" mode="linear_format">
    <xsl:choose>
      <xsl:when test="(position() = '1' ) ">
        <xsl:choose>
          <xsl:when test="current()/Coefficient != '1' and current()/Coefficient != '-1' ">
            <xsl:value-of select="concat(*[2], '*', *[1])" />
          </xsl:when>
          <xsl:otherwise>
            <xsl:if test="current()/Coefficient = '-1' ">
              <xsl:value-of select="concat('-', *[1])" />
            </xsl:if>
            <xsl:if test="current()/Coefficient = '1' ">
              <xsl:value-of select="concat(*[1])" />
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <!-- coefficient checking : positive or negative if it is not the first one -->
        <xsl:if test="current()/Coefficient = '-1' ">
          <xsl:value-of select="concat('-', *[1])" />
        </xsl:if>
        <xsl:if test="current()/Coefficient = '1' ">
          <xsl:value-of select="concat('+',*[1])" />
        </xsl:if>
        <xsl:if test="current()/Coefficient &gt; '1' ">
          <xsl:value-of select="concat('+',*[2], '*', *[1])" />
        </xsl:if>
        <xsl:if test="current()/Coefficient &lt; '-1' ">
          <xsl:value-of select="concat(*[2], '*', *[1])" />
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*" mode="field_range_format">
    <xsl:value-of select="concat(*[1], '..', *[2])" />
  </xsl:template>

  <xsl:template match="*" mode="congruence_format">
    <xsl:choose>
      <!--     print nothing when congruences do not have any information -->
      <xsl:when test="*[1] = '1' and *[2] = '0'" />
      <xsl:when test="*[1] = '0'">
        <xsl:value-of select="concat(*[2], ' (c)')" />
      </xsl:when>
      <xsl:when test="*[2] = '0'">
        <xsl:value-of select="concat('*', *[1])" />
      </xsl:when>
      <xsl:when test="*[2] &lt; '0'">
        <xsl:value-of select="concat('*', *[1], *[2])" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat('*', *[1], '+', *[2])" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*" mode="pointer_format">
    <xsl:value-of select="concat(*[1], '*', *[2])" />
  </xsl:template>

  <xsl:template match="*" mode="interval_format">
    <xsl:choose>
      <xsl:when test="current()/lowerBound = current()/upperBound">
        <xsl:value-of select="*[1]" />
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="concat('[', *[1], ',', *[2], ']')" />
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

</xsl:stylesheet>