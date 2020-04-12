package crud.models

import java.time.Year
import java.util.UUID

final case class BookInsert(authorId: UUID, title: String, year: Year, genre: String)
