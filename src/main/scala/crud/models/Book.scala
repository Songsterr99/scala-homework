package crud.models

import java.time.Year
import java.util.UUID

final case class Book(id: UUID, author: Author, title: String, year: Year, genre: String)
