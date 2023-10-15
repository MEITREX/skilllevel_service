# Skilllevel Service API

<details>
  <summary><strong>Table of Contents</strong></summary>

  * [Query](#query)
  * [Mutation](#mutation)
  * [Objects](#objects)
    * [PaginationInfo](#paginationinfo)
    * [SkillLevel](#skilllevel)
    * [SkillLevelLogItem](#skilllevellogitem)
    * [SkillLevels](#skilllevels)
  * [Inputs](#inputs)
    * [DateTimeFilter](#datetimefilter)
    * [IntFilter](#intfilter)
    * [Pagination](#pagination)
    * [StringFilter](#stringfilter)
  * [Enums](#enums)
    * [SortDirection](#sortdirection)
  * [Scalars](#scalars)
    * [Boolean](#boolean)
    * [Date](#date)
    * [DateTime](#datetime)
    * [Float](#float)
    * [Int](#int)
    * [LocalTime](#localtime)
    * [String](#string)
    * [Time](#time)
    * [UUID](#uuid)
    * [Url](#url)

</details>

## Query
<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>_internal_noauth_userSkillLevelsByChapterIds</strong></td>
<td valign="top">[<a href="#skilllevels">SkillLevels</a>!]!</td>
<td>


Get the skill levels of the current user for all skill types for a list of chapter ids.
⚠️ This query is only accessible internally in the system and allows the caller to fetch contents without
any permissions check and should not be called without any validation of the caller's permissions. ⚠️

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">chapterIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>_internal_noauth_skillLevelsForUserByChapterIds</strong></td>
<td valign="top">[<a href="#skilllevels">SkillLevels</a>!]!</td>
<td>


Get the skill levels of the specified user for all skill types for a list of chapter ids.
⚠️ This query is only accessible internally in the system and allows the caller to fetch contents without
any permissions check and should not be called without any validation of the caller's permissions. ⚠️

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">chapterIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

## Mutation
<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>recalculateLevels</strong> ⚠️</td>
<td valign="top"><a href="#skilllevels">SkillLevels</a>!</td>
<td>


  ONLY FOR TESTING PURPOSES. DO NOT USE IN FRONTEND. WILL BE REMOVED.

  Triggers the recalculation of the skill level of the user.
  This is done automatically at some time in the night.

  The purpose of this mutation is to allow testing of the skill level score and demonstrate the functionality.
  🔒 The user must be a super-user, otherwise an exception is thrown.

<p>⚠️ <strong>DEPRECATED</strong></p>
<blockquote>

Only for testing purposes. Will be removed.

</blockquote>
</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">chapterId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

## Objects

### PaginationInfo


Return type for information about paginated results.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>page</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The current page number.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>size</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The number of elements per page.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>totalElements</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The total number of elements across all pages.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>totalPages</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The total number of pages.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>hasNext</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


Whether there is a next page.

</td>
</tr>
</tbody>
</table>

### SkillLevel


The skill level of a user.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>value</strong></td>
<td valign="top"><a href="#float">Float</a>!</td>
<td>


The value of the skill level.
levels are between 0 and 100.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>log</strong></td>
<td valign="top">[<a href="#skilllevellogitem">SkillLevelLogItem</a>!]!</td>
<td>


A log of the changes to the skill level

</td>
</tr>
</tbody>
</table>

### SkillLevelLogItem


An item in the skill level change log.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>date</strong></td>
<td valign="top"><a href="#datetime">DateTime</a>!</td>
<td>


The date when the skill level changed.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>difference</strong></td>
<td valign="top"><a href="#float">Float</a>!</td>
<td>


The difference between the previous and the new skill level.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>oldValue</strong></td>
<td valign="top"><a href="#float">Float</a>!</td>
<td>


The old skill level.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>newValue</strong></td>
<td valign="top"><a href="#float">Float</a>!</td>
<td>


The new skill level.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>associatedContentIds</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


The ids of the contents that are associated with the change.

</td>
</tr>
</tbody>
</table>

### SkillLevels


The four skill level of a user.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>remember</strong></td>
<td valign="top"><a href="#skilllevel">SkillLevel</a>!</td>
<td>


remember represents how much user remember the concept

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>understand</strong></td>
<td valign="top"><a href="#skilllevel">SkillLevel</a>!</td>
<td>


understand represents how well the user understands learned content.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>apply</strong></td>
<td valign="top"><a href="#skilllevel">SkillLevel</a>!</td>
<td>


apply represents the how well user applies the learned concept during assessment.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>analyze</strong></td>
<td valign="top"><a href="#skilllevel">SkillLevel</a>!</td>
<td>


apply is how much user can evaluate information and draw conclusions

</td>
</tr>
</tbody>
</table>

## Inputs

### DateTimeFilter


Filter for date values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>after</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


If specified, filters for dates after the specified value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>before</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


If specified, filters for dates before the specified value.

</td>
</tr>
</tbody>
</table>

### IntFilter


Filter for integer values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>equals</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


An integer value to match exactly.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>greaterThan</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


If specified, filters for values greater than to the specified value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>lessThan</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


If specified, filters for values less than to the specified value.

</td>
</tr>
</tbody>
</table>

### Pagination


Specifies the page size and page number for paginated results.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>page</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The page number, starting at 0.
If not specified, the default value is 0.
For values greater than 0, the page size must be specified.
If this value is larger than the number of pages, an empty page is returned.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>size</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The number of elements per page.

</td>
</tr>
</tbody>
</table>

### StringFilter


Filter for string values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>equals</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>


A string value to match exactly.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contains</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>


A string value that must be contained in the field that is being filtered.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>ignoreCase</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


If true, the filter is case-insensitive.

</td>
</tr>
</tbody>
</table>

## Enums

### SortDirection


Specifies the sort direction, either ascending or descending.

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>ASC</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>DESC</strong></td>
<td></td>
</tr>
</tbody>
</table>

## Scalars

### Boolean

Built-in Boolean

### Date

An RFC-3339 compliant Full Date Scalar

### DateTime

A slightly refined version of RFC-3339 compliant DateTime Scalar

### Float

Built-in Float

### Int

Built-in Int

### LocalTime

24-hour clock time value string in the format `hh:mm:ss` or `hh:mm:ss.sss`.

### String

Built-in String

### Time

An RFC-3339 compliant Full Time Scalar

### UUID

A universally unique identifier compliant UUID Scalar

### Url

A Url scalar

